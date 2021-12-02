package org.skyve.impl.metadata.repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.skyve.impl.generate.ViewGenerator;
import org.skyve.impl.metadata.customer.CustomerImpl;
import org.skyve.impl.metadata.model.document.DocumentImpl;
import org.skyve.impl.metadata.repository.customer.CustomerMetaData;
import org.skyve.impl.metadata.repository.document.DocumentMetaData;
import org.skyve.impl.metadata.repository.module.ModuleMetaData;
import org.skyve.impl.metadata.repository.router.Router;
import org.skyve.impl.metadata.repository.view.ViewMetaData;
import org.skyve.impl.metadata.user.ActionPrivilege;
import org.skyve.impl.metadata.user.Privilege;
import org.skyve.impl.metadata.user.RoleImpl;
import org.skyve.impl.metadata.view.ViewImpl;
import org.skyve.impl.util.UtilImpl;
import org.skyve.metadata.MetaData;
import org.skyve.metadata.MetaDataException;
import org.skyve.metadata.customer.Customer;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.module.Module;
import org.skyve.metadata.module.Module.DocumentRef;
import org.skyve.metadata.repository.MutableRepository;
import org.skyve.metadata.repository.OnDemandRepository;
import org.skyve.metadata.user.Role;
import org.skyve.metadata.view.View;
import org.skyve.metadata.view.View.ViewType;

public abstract class MutableCachedRepository extends ProvidedRepositoryDelegate implements MutableRepository, OnDemandRepository {
	/**
	 * The cache.
	 * MetaData namespace and name -> MetaData.
	 * eg customers/bizhub
	 *    modules/admin
	 *    modules/admin/
	 * Thread-safe and performant for mostly-read operations.
	 */
	private ConcurrentHashMap<String, Optional<MetaData>> cache = new ConcurrentHashMap<>();
	
	protected static final String ROUTER_KEY = ROUTER_NAMESPACE + ROUTER_NAME;

	@Override
	public void evictCachedMetaData(Customer customer) {
		// Clear the lot
		if (customer == null) {
			cache.clear();
		}
		else {
			// Clear any customer overrides and any modules the customer has access to.
			List<String> moduleNames = ((CustomerImpl) customer).getModuleNames();
			List<String> modulePrefixes = new ArrayList<>(moduleNames.size());
			for (String moduleName : moduleNames) {
				modulePrefixes.add(MODULES_NAMESPACE + moduleName);
			}
			
			Iterator<String> i = cache.keySet().iterator();
			final String customerOverridePrefix = CUSTOMERS_NAMESPACE + customer.getName();
			while (i.hasNext()) {
				String key = i.next();
				if (key.startsWith(customerOverridePrefix)) {
					i.remove();
					continue;
				}
				for (String modulePrefix : modulePrefixes) {
					if (key.startsWith(modulePrefix)) {
						i.remove();
						break;
					}
				}
			}
		}
	}
	
	@Override
	public Router getRouter() {
		Router result = null;
		
		if (UtilImpl.DEV_MODE) {
			// Cater for the situation where setRouter has been called
			Optional<MetaData> o = cache.get(ROUTER_KEY);
			if ((o != null) && o.isEmpty()) { // not a cache miss
				result = loadRouter();
				result = result.convert(ROUTER_NAME, getDelegator());
			}
		}
		else {
			Optional<MetaData> o = cache.computeIfPresent(ROUTER_KEY, (k, v) -> {
				if (v.isEmpty()) {
					Router router = loadRouter();
					router = router.convert(ROUTER_NAME, getDelegator());
					return Optional.of(router);
				}
				return v;
			});
			if ((o != null) && o.isPresent()) {
				result = (Router) o.get();
			}
		}
		
		return result;
	}
	
	@Override
	public Router setRouter(Router router) {
		Router result = router.convert(ROUTER_NAME, getDelegator());
		// Ignore dev mode flag here as we need to seed the cache in this method.
		cache.put(ROUTER_KEY, Optional.of(result));
		return result;
	}

	@Override
	public Customer getCustomer(String customerName) {
		String customerKey = CUSTOMERS_NAMESPACE + customerName;
		Optional<MetaData> o = cache.computeIfPresent(customerKey, (k, v) -> {
			if (v.isEmpty()) {
				CustomerMetaData customerMetaData = loadCustomer(customerName);
				Customer customer = customerMetaData.convert(customerName, getDelegator());
				return Optional.of(customer);
			}
			return v;
		});
		return ((o == null) || o.isEmpty()) ? null : (Customer) o.get();
	}

	@Override
	public Customer addCustomer(CustomerMetaData customer) {
		String customerName = customer.getName();
		Customer result = customer.convert(customerName, getDelegator());
		cache.put(CUSTOMERS_NAMESPACE + customerName, Optional.of(result));
		return result;
	}

	@Override
	public Module getModule(Customer customer, String moduleName) {
		Module result = null;
		if (customer != null) {
			if (! ((CustomerImpl) customer).getModuleNames().contains(moduleName)) {
				throw new MetaDataException("Module " + moduleName + " does not exist for customer " + customer.getName());
			}
			// get customer overridden
			result = getModuleInternal(customer, moduleName);
		}
		if (result == null) { // not overridden
			result = getModuleInternal(null, moduleName);
		}
		return result;
	}
	
	private Module getModuleInternal(Customer customer, String moduleName) {
		final String customerName = (customer == null) ? null : customer.getName();
		StringBuilder moduleKey = new StringBuilder(64);
		if (customerName != null) {
			moduleKey.append(CUSTOMERS_NAMESPACE).append(customerName).append('/');
		}
		moduleKey.append(MODULES_NAMESPACE).append(moduleName);
		
		Optional<MetaData> result = cache.computeIfPresent(moduleKey.toString(), (k, v) -> {
			if (v.isEmpty()) {
				ModuleMetaData moduleMetaData = loadModule(customerName, moduleName);
				Module module = this.convertModule(customerName, moduleName, moduleMetaData);
				return Optional.of(module);
			}
			return v;
		});
		if ((result != null) && result.isPresent()) {
			return (Module) result.get();
		}
		return null;
	}

	private Module convertModule(String customerName, String moduleName, ModuleMetaData module) {
		String metaDataName = null;
		if (customerName != null) {
			metaDataName = new StringBuilder(64).append(moduleName).append(" (").append(customerName).append(')').toString();
		}
		else {
			metaDataName = moduleName;
		}
		return module.convert(metaDataName, getDelegator());
	}
	
	@Override
	public Module addModule(Customer customer, ModuleMetaData module) {
		String customerName = customer.getName();
		String moduleName = module.getName();
		Module result = convertModule(customerName, moduleName, module);
		
		StringBuilder moduleKey = new StringBuilder(64);
		moduleKey.append(CUSTOMERS_NAMESPACE).append(customerName).append('/').append(MODULES_NAMESPACE).append(moduleName);
		cache.put(moduleKey.toString(), Optional.of(result));
		
		return result;
	}
	
	@Override
	public Module addModule(ModuleMetaData module) {
		String moduleName = module.getName();

		Module result = convertModule(null, moduleName, module);
		cache.put(MODULES_NAMESPACE + moduleName, Optional.of(result));
		
		return result;
	}
	
	@Override
	public Document getDocument(Customer customer, Module module, String documentName) {
		Document result = null;
		if (customer != null) {
			// get customer overridden
			result = getDocumentInternal(customer, module, documentName);
		}
		if (result == null) { // not overridden
			result = getDocumentInternal(null, module, documentName);
		}
		return result;
	}

	private Document getDocumentInternal(Customer customer, Module module, String documentName) {
		DocumentRef ref = module.getDocumentRefs().get(documentName);
		if (ref == null) {
			throw new IllegalArgumentException(documentName + " does not exist for this module - " + module.getName());
		}
		String documentModuleName = ((ref.getReferencedModuleName() == null) ? module.getName() : ref.getReferencedModuleName());

		final String customerName = (customer == null) ? null : customer.getName();
		StringBuilder documentKey = new StringBuilder(64);
		if (customerName != null) {
			documentKey.append(CUSTOMERS_NAMESPACE).append(customerName).append('/');
		}
		documentKey.append(MODULES_NAMESPACE).append(documentModuleName).append('/').append(documentName);
		
		Optional<MetaData> result = cache.computeIfPresent(documentKey.toString(), (k, v) -> {
			if (v.isEmpty()) {
				DocumentMetaData documentMetaData = loadDocument(customerName, documentModuleName, documentName);
				Module documentModule = getModule(customer, documentModuleName);
				Document document = this.convertDocument(customerName, documentModuleName, documentModule, documentName, documentMetaData);
				return Optional.of(document);
			}
			return v;
		});
		if ((result != null) && result.isPresent()) {
			return (Document) result.get();
		}
		return null;
	}

	private Document convertDocument(String customerName,
										String moduleName,
										Module module,
										String documentName,
										DocumentMetaData document) {
		StringBuilder metaDataName = new StringBuilder(128);
		metaDataName = new StringBuilder(64).append(moduleName).append('.').append(documentName);
		if (customerName != null) {
			metaDataName.append(" (").append(customerName).append(')');
		}
		Document result = document.convert(metaDataName.toString(), getDelegator());
		
		DocumentImpl internalResult = (DocumentImpl) result;
		internalResult.setOwningModuleName(moduleName);

		// check each document reference query name links to a module query
		for (String referenceName : result.getReferenceNames()) {
			String queryName = result.getReferenceByName(referenceName).getQueryName();
			if ((queryName != null) && (module.getMetaDataQuery(queryName) == null)) {
				StringBuilder mde = new StringBuilder(documentName);
				mde.append(" : The reference ");
				mde.append(referenceName);
				mde.append(" has a query ");
				mde.append(queryName);
				mde.append(" that does not exist in module ");
				if (customerName != null) {
					mde.append(customerName);
					mde.append(".");
				}
				mde.append(moduleName);
				
				throw new MetaDataException(mde.toString());
			}
		}

		// Add actions in privileges to the document to enable good view generation
		for (Role role : module.getRoles()) {
			for (Privilege privilege : ((RoleImpl) role).getPrivileges()) {
				if (privilege instanceof ActionPrivilege) {
					ActionPrivilege actionPrivilege = (ActionPrivilege) privilege;
					if (actionPrivilege.getDocumentName().equals(result.getName())) {
						internalResult.getDefinedActionNames().add(actionPrivilege.getName());
					}
				}
			}
		}
		
		return result;
	}
	
	@Override
	public Document addDocument(Customer customer, Module module, DocumentMetaData document) {
		String customerName = customer.getName();
		String moduleName = module.getName();
		String documentName = document.getName();
		Document result = convertDocument(customerName, moduleName, module, documentName, document);
		
		StringBuilder documentKey = new StringBuilder(64);
		documentKey.append(CUSTOMERS_NAMESPACE).append(customerName).append('/').append(MODULES_NAMESPACE).append(moduleName).append('/').append(documentName);
		cache.put(documentKey.toString(), Optional.of(result));
		
		return result;
	}

	@Override
	public Document addDocument(Module module, DocumentMetaData document) {
		String moduleName = module.getName();
		String documentName = document.getName();
		Document result = convertDocument(null, moduleName, module, documentName, document);
		
		StringBuilder documentKey = new StringBuilder(64);
		documentKey.append(MODULES_NAMESPACE).append(moduleName).append('/').append(documentName);
		cache.put(documentKey.toString(), Optional.of(result));
		
		return result;
	}

	@Override
	public View getView(String uxui,
							Customer customer, 
							Document document, 
							String name) {
		View result = null;
		String customerName = null;
		if (customer != null) {
			customerName = customer.getName();
			// get customer overridden
			if (uxui != null) {
				// get uxui specific
				result = getViewInternal(customerName, customer, document, uxui, name);
			}
			if (result == null) {
				result = getViewInternal(customerName, customer, document, null, name);
			}
		}
		
		if (result == null) { // not overridden
			if (uxui != null) {
				// get uxui specific
				result = getViewInternal(null, customer, document, uxui, name);
			}
			if (result == null) {
				result = getViewInternal(null, customer, document, null, name);
			}
		}
		
		// scaffold
		if ((result == null) && getUseScaffoldedViews()) {
			if (UtilImpl.DEV_MODE) {
				result = scaffoldView(customer, document, name);
			}
			else {
				StringBuilder key = new StringBuilder(128);
				String documentModuleName = document.getOwningModuleName();
				String documentName = document.getName();
				key.append(MODULES_NAMESPACE).append(documentModuleName).append('/');
				key.append(documentName).append('/').append(VIEWS_NAMESPACE).append(name);
				Optional<MetaData> o = cache.computeIfAbsent(key.toString(), k -> {
					View view = scaffoldView(customer, document, name);
					return (view == null) ? null : Optional.of(view);
				});
				if ((o != null) && o.isPresent()) {
					result = (View) o.get();
				}
			}
		}
		return result;
	}

	private View getViewInternal(String customerName, Customer customer, Document document, String uxui, String viewName) {
		StringBuilder viewKey = new StringBuilder(128);
		if (customerName != null) {
			viewKey.append(CUSTOMERS_NAMESPACE).append(customerName).append('/');
		}
		viewKey.append(MODULES_NAMESPACE);
		String documentModuleName = document.getOwningModuleName();
		String documentName = document.getName();
		viewKey.append(documentModuleName).append('/').append(documentName).append('/').append(VIEWS_NAMESPACE);
		if (uxui != null) {
			viewKey.append(uxui).append('/');
		}
		viewKey.append(viewName);
		
		View result = null;
		if (UtilImpl.DEV_MODE) {
			// Cater for the situation where setView has been called
			Optional<MetaData> o = cache.get(viewKey.toString());
			if ((o != null) && o.isEmpty()) { //  not a cache miss
				ViewMetaData viewMetaData = loadView(customerName, documentModuleName, documentName, uxui, viewName);
				if (viewMetaData != null) {
					result = this.convertView(customerName, customer, documentModuleName, documentName, document, uxui, viewMetaData);
				}
			}
		}
		else {
			Optional<MetaData> o = cache.computeIfPresent(viewKey.toString(), (k, v) -> {
				if (v.isEmpty()) {
					ViewMetaData viewMetaData = loadView(customerName, documentModuleName, documentName, uxui, viewName);
					View view = null;
					if (viewMetaData != null) {
						view = this.convertView(customerName, customer, documentModuleName, documentName, document, uxui, viewMetaData);
					}
					return Optional.ofNullable(view);
				}
				return v;
			});
			if ((o != null) && o.isPresent()) {
				result = (View) o.get();
			}
		}
		
		return result;
	}

	private View convertView(String customerName,
								Customer customer,
								String moduleName,
								String documentName,
								Document document,
								String uxui,
								ViewMetaData view) {
		StringBuilder metaDataName = new StringBuilder(128);
		metaDataName.append(moduleName).append('.').append(documentName).append('.');
		if (uxui != null) {
			metaDataName.append(uxui).append('.');
		}
		metaDataName.append(view.getName());
		if (customerName != null) {
			metaDataName.append(" (").append(customerName).append(')');
		}
		
		ViewImpl result = view.convert(metaDataName.toString(), getDelegator());
		result.resolve(uxui, customer, document);
		return result;
	}
	
	private View scaffoldView(Customer customer, Document document, String viewName) {
		if (ViewType.edit.toString().equals(viewName) || 
				ViewType.pick.toString().equals(viewName) || 
				ViewType.params.toString().equals(viewName)) {
			return new ViewGenerator(this).generate(customer, document, viewName);
		}
		return null;
	}

	@Override
	public View addView(Customer customer, String uxui, Document document, ViewMetaData view) {
		String customerName = customer.getName();
		String moduleName = document.getOwningModuleName();
		String documentName = document.getName();
		View result = convertView(customerName, customer, moduleName, documentName, document, uxui, view);
		
		StringBuilder viewKey = new StringBuilder(128);
		viewKey.append(CUSTOMERS_NAMESPACE).append(customerName).append('/');
		viewKey.append(MODULES_NAMESPACE).append(moduleName).append('/');
		viewKey.append(documentName).append('/').append(uxui).append('/');
		viewKey.append(VIEWS_NAMESPACE).append(view.getName());
		cache.put(viewKey.toString(), Optional.of(result));
		
		return result;
	}

	@Override
	public View addView(String uxui, Document document, ViewMetaData view) {
		String moduleName = document.getOwningModuleName();
		String documentName = document.getName();
		View result = convertView(null, null, moduleName, documentName, document, uxui, view);
		
		StringBuilder viewKey = new StringBuilder(128);
		viewKey.append(MODULES_NAMESPACE).append(moduleName).append('/');
		viewKey.append(documentName).append('/').append(uxui).append('/');
		viewKey.append(VIEWS_NAMESPACE).append(view.getName());
		cache.put(viewKey.toString(), Optional.of(result));
		
		return result;
	}
	
	@Override
	public View addView(Customer customer, Document document, ViewMetaData view) {
		String customerName = customer.getName();
		String moduleName = document.getOwningModuleName();
		String documentName = document.getName();
		View result = convertView(customerName, customer, moduleName, documentName, document, null, view);
		
		StringBuilder viewKey = new StringBuilder(128);
		viewKey.append(CUSTOMERS_NAMESPACE).append(customerName).append('/');
		viewKey.append(MODULES_NAMESPACE).append(moduleName).append('/');
		viewKey.append(documentName).append('/');
		viewKey.append(VIEWS_NAMESPACE).append(view.getName());
		cache.put(viewKey.toString(), Optional.of(result));
		
		return result;
	}
	
	@Override
	public View addView(Document document, ViewMetaData view) {
		String moduleName = document.getOwningModuleName();
		String documentName = document.getName();
		View result = convertView(null, null, moduleName, documentName, document, null, view);
		
		StringBuilder viewKey = new StringBuilder(128);
		viewKey.append(MODULES_NAMESPACE).append(moduleName).append('/');
		viewKey.append(documentName).append('/');
		viewKey.append(VIEWS_NAMESPACE).append(view.getName());
		cache.put(viewKey.toString(), Optional.of(result));
		
		return result;
	}
	
	/**
	 * Called by populateKeys() implementations.
	 * @param key
	 */
	protected void addKey(String key) {
		cache.putIfAbsent(key, Optional.empty());
	}
	
	@Override
	public String vtable(String customerName, String key) {
		String result = new StringBuilder(128).append(CUSTOMERS_NAMESPACE).append(customerName).append('/').append(key).toString();
		if (! cache.containsKey(result)) {
			if (cache.containsKey(key)) {
				result = key;
			}
			else {
				result = null;
			}
		}
		return result;
	}
}