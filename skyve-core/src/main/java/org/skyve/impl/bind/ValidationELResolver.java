package org.skyve.impl.bind;

import java.beans.FeatureDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.MethodNotFoundException;
import javax.el.PropertyNotFoundException;

import org.apache.commons.beanutils.PropertyUtils;
import org.skyve.impl.metadata.model.document.DocumentImpl;
import org.skyve.impl.metadata.model.document.InverseMany;
import org.skyve.metadata.customer.Customer;
import org.skyve.metadata.model.Attribute;
import org.skyve.metadata.model.Extends;
import org.skyve.metadata.model.document.Collection;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.model.document.Relation;
import org.skyve.metadata.module.Module;

/**
 * Used to Resolve to classes/Documents/singletonList(Document) when walking an EL AST.
 * This class provides extra type safety across Collections/InverseManys when evaluating/traversing 
 * Skyve domain objects.
 * If ELExpressionEvaluator.validate() is called with Object.class, no type safety is checked.
 * If ELExpressionEvaluator.validate() is called with a Skyve Document or other classes, static type checking is done.
 * NB Static type checking may come undone where expressions assume polymorphic instances exist.
 * @author mike
 */
class ValidationELResolver extends ELResolver {
	private static final Class<?> UNMODIFIABLE_LIST_CLASS = Collections.unmodifiableList(new ArrayList<>()).getClass();
	private static final Class<?> UNMODIFIABLE_MAP_CLASS = Collections.unmodifiableMap(new HashMap<>()).getClass();
	
	private Customer customer;
	
	ValidationELResolver(Customer customer) {
		this.customer = customer;
	}

	@Override
	public Class<?> getType(ELContext context, Object base, Object property) {
		System.out.println("getType " + base + ", " + property);
		Class<?> type = getType(base, property);
		if (type != null) {
			context.setPropertyResolved(true);
		}
		return type;
	}

	@Override
	public Object getValue(ELContext context, Object base, Object property) {
		System.out.println("getValue " + base + ", " + property);
		Object value = getClassOrDocument(base, property);
		if (value != null) {
			context.setPropertyResolved(base, property);
		}
		return value;
	}
	
	@Override
	public void setValue(ELContext context, Object base, Object property, Object val) {
		System.out.println("setValue " + base + ", " + property + ", " + val);
		Class<?> type = getType(base, property);
		if (type != null) {
			if (val == null) {
				if (type.isPrimitive()) {
					throw new PropertyNotFoundException("Cannot set a value of null on primitive type " + type + " for property " + property + " on object " + base);
				}
				context.setPropertyResolved(base, property);
			}
			else {
				if (Object.class.equals(type)) { // we are not in type-safe mode
					context.setPropertyResolved(base, property);
				}
				else if (type.isAssignableFrom(val.getClass())) {
					context.setPropertyResolved(base, property);
				}
				else {
					throw new PropertyNotFoundException("Property " + property + " cannot be set to value " + val);
				}
			}
		}
	}

	/**
	 * This method returns 
	 * <ul>
	 * 	<li>a document if given a document as a base and the property is an association or inverseOne.</li>
	 * 	<li>a singleton list of document if given a document as a base and the property is a collection or inverseMany.</li>
	 * 	<li>Object.class if base is Object.class (in non type-safe mode)</li>
	 * 	<li>The array component type if base is an array (property is checked for integer)</li>
	 * 	<li>Object.class if base is a List (property is check for integer)</li>
	 * 	<li>Object.class if base is a Map</li>
	 * 	<li>Otherwise the Bean Introspector's idea on what the property type is</li>
	 * </ul>
	 * @param base
	 * @param property
	 * @return Document, List or Class
	 */
	private Object getClassOrDocument(Object base, Object property) {
		System.out.println("getValue " + base + ", " + property);
		Object object = base;
		final String propertyName = (String) property;

		// Possible Collection or InverseMany
		// If so, return the single element in the list which is the related document
		if (object instanceof List<?>) {
			List<?> list = (List<?>) object;
			if (list.size() == 1) {
				Object e = list.get(0);
				if (e instanceof DocumentImpl) {
					checkInteger(property);
					return e;
				}
			}
		}
		else if (object instanceof DocumentImpl) {
			DocumentImpl document = (DocumentImpl) object;

			Document currentDocument = document;
			while (currentDocument != null) {
				// Conditions are boolean type
				if (currentDocument.getCondition(propertyName) != null) {
					return Boolean.TYPE;
				}

				Attribute attribute = currentDocument.getAttribute(propertyName);
				if (attribute != null) {
					if (attribute instanceof Relation) {
						String relationDocumentName = ((Relation) attribute).getDocumentName();
						Module module = customer.getModule(currentDocument.getOwningModuleName());
						DocumentImpl relationDocument = (DocumentImpl) module.getDocument(customer, relationDocumentName);
						// Collection and InverseMany are a singleton List<Document>
						if ((attribute instanceof Collection) || (attribute instanceof InverseMany)) {
							return Collections.singletonList(relationDocument);
						}
						// Associations and InverseOne return the related Document
						return relationDocument;
					}
					// Every other attribute is a scalar type
					return attribute.getAttributeType().getImplementingType();
				}
				
				// Get super-document if applicable
				Extends currentExtends = currentDocument.getExtends();
				if (currentExtends == null) {
					currentDocument = null;
				}
				else {
					Module module = customer.getModule(currentDocument.getOwningModuleName());
					currentDocument = module.getDocument(customer, currentExtends.getDocumentName());
				}
			}			

			// If we are here, then the property name is not a condition or attribute.
			// Set the object to the domain class and try below.
			try {
				object = document.getBeanClass(customer);
			}
			catch (ClassNotFoundException e) {
				throw new IllegalStateException("Cannot get bean class for document " + document.getOwningModuleName() + "." + document.getName(), e);
			}
		}
		
		if (object instanceof Class<?>) {
			Class<?> type = (Class<?>) object;
			if (Object.class.equals(type)) { // we are not in type-safe mode
				return Object.class;
			}
			else if (type.isArray()) {
				checkInteger(property);
				return type.getComponentType();
			}
			else if (List.class.isAssignableFrom(type)) {
				checkInteger(property);
				return Object.class;
			}
			else if (Map.class.isAssignableFrom(type)) {
				return Object.class;
			}
			else { // try a bean property
				for (PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(type)) {
					if (descriptor.getName().equals(propertyName)) {
						return descriptor.getPropertyType();
					}
				}
				throw new PropertyNotFoundException("Property " + propertyName + " on " + base + " is not public");
			}
		}
		
		return null; // we don't handle it
	}

	private Class<?> getType(Object base, Object property) {
		Class<?> result = null;
		
		Object value = getClassOrDocument(base, property);
		if (value instanceof Class<?>) {
			result = (Class<?>) value;
		}
		else if (value instanceof List<?>) {
			result = List.class;
		}
		else if (value instanceof DocumentImpl) {
			DocumentImpl document = (DocumentImpl) value;
			try {
				result = document.getBeanClass(customer);
			}
			catch (ClassNotFoundException e) {
				throw new IllegalStateException("Cannot get bean class for document " + document.getOwningModuleName() + "." + document.getName(), e);
			}
		}
		
		return result;
	}
	
	@Override
	public Object invoke(ELContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {
		System.out.println("invoke " + base + ", " + method + ", " + paramTypes + ", " + params);
		Class<?> type = null;
		if (Object.class.equals(base)) { // we are not in type-safe mode
			context.setPropertyResolved(base, method);
			return Object.class;
		}
		else if (base instanceof List<?>) {
			type = List.class;
		}
		else if (base instanceof DocumentImpl) {
			try {
				type = ((DocumentImpl) base).getBeanClass(customer);
			}
			catch (ClassNotFoundException e) {
				throw new MethodNotFoundException("Method " + method + " on " + base + " cannot be invoked", e);
			}
		}
		else if (base instanceof Class<?>) {
			type = (Class<?>) base;
		}
		else {
			return null; // we don't handle this
		}
		
		try {
			if (paramTypes != null) {
				Method m = type.getMethod((String) method, paramTypes);
				if (Modifier.isPublic(m.getModifiers())) {
					context.setPropertyResolved(base, method);
					return m.getReturnType();
	            }
				throw new MethodNotFoundException("Method " + method + " on " + base + " is not public");
			}
			
			int paramCount = (params == null) ? 0 : params.length;
			for (Method m : type.getMethods()) {
				if (m.getName().equals(method) && 
						(m.isVarArgs() || (m.getParameterTypes().length == paramCount))) {
					context.setPropertyResolved(base, method);
					return m.getReturnType();
				}
			}
			throw new MethodNotFoundException("Method " + method + " on " + base + " does not exist");
		}
		catch (NoSuchMethodException e) {
			throw new MethodNotFoundException("Method " + method + " on " + base + " does not exist", e);
		}
	}

	@Override
	public boolean isReadOnly(ELContext context, Object base, Object property) {
		System.out.println("isReadOnly " + base + ", " + property);

		if (Object.class.equals(base)) { // we are not in type-safe mode
			context.setPropertyResolved(true);
			return false;
		}
		else if (base instanceof List<?>) {
			checkInteger(property);
			context.setPropertyResolved(true);
			return UNMODIFIABLE_LIST_CLASS.equals(((List<?>) base).getClass());
		}
		
		Object object = base;
		String propertyName = (String) property;
		if (object instanceof DocumentImpl) {
			DocumentImpl document = (DocumentImpl) object;

			Document currentDocument = document;
			while (currentDocument != null) {
				// Conditions are read-only
				if (currentDocument.getCondition(propertyName) != null) {
					context.setPropertyResolved(true);
					return true;
				}

				Attribute attribute = currentDocument.getAttribute(propertyName);
				if (attribute != null) {
					// Attributes are writable except Collection and InverseMany are read-only
					context.setPropertyResolved(true);
					return (attribute instanceof Collection) || (attribute instanceof InverseMany);
				}
				
				// Get super-document if applicable
				Extends currentExtends = currentDocument.getExtends();
				if (currentExtends == null) {
					currentDocument = null;
				}
				else {
					Module module = customer.getModule(currentDocument.getOwningModuleName());
					currentDocument = module.getDocument(customer, currentExtends.getDocumentName());
				}
			}			

			// If we are here, then the property name is not a condition or attribute.
			// Set the object to the domain class and try below.
			try {
				object = document.getBeanClass(customer);
			}
			catch (ClassNotFoundException e) {
				throw new IllegalStateException("Cannot get bean class for document " + document.getOwningModuleName() + "." + document.getName(), e);
			}
		}
		
		if (object instanceof Class<?>) {
			context.setPropertyResolved(true);
			Class<?> type = (Class<?>) object;
			if (type.isArray()) {
				checkInteger(property);
				return false;
			}
			else if (List.class.isAssignableFrom(type)) {
				checkInteger(property);
				return UNMODIFIABLE_LIST_CLASS.equals(type);
			}
			else if (Map.class.isAssignableFrom(type)) {
				return UNMODIFIABLE_MAP_CLASS.equals(type);
			}
			else { // try a bean property
				for (PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(type)) {
					if (descriptor.getName().equals(propertyName)) {
						return (descriptor.getWriteMethod() == null);
					}
				}
				throw new PropertyNotFoundException("Bean property " + propertyName + " not found in " + type);
			}
		}

		return false;
	}

	@Override
	public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
		return null;
	}

	@Override
	public Class<?> getCommonPropertyType(ELContext context, Object base) {
		System.out.println("getCommonPropertyType " + base);

		if (base instanceof DocumentImpl) {
			return Object.class;
		}
		else if (base instanceof List<?>) {
			return Integer.class;
		}
		else if (base instanceof Class<?>) {
			Class<?> type = (Class<?>) base;
			if (type.isArray()) {
				return Integer.class;
			}
			else if (List.class.isAssignableFrom(type)) {
				return Integer.class;
			}
			return Object.class;
		}

		return null;
	}

	private static void checkInteger(Object p) {
		if (! (p instanceof Number)) {
			if (p instanceof String) {
				Integer.parseInt((String) p); // throws NumberFormatException
			}
			else if (! (p instanceof Character)) {
				throw new PropertyNotFoundException(p + " is not coercible to an integer");
			}
		}
	}
}
