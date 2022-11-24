package org.skyve.impl.metadata.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.skyve.domain.Bean;
import org.skyve.domain.ChildBean;
import org.skyve.impl.bind.BindUtil;
import org.skyve.impl.metadata.Container;
import org.skyve.impl.metadata.customer.CustomerImpl;
import org.skyve.impl.metadata.model.document.DocumentImpl;
import org.skyve.impl.metadata.module.ModuleImpl;
import org.skyve.impl.metadata.repository.view.access.ViewUserAccessMetaData;
import org.skyve.impl.metadata.repository.view.access.ViewUserAccessUxUiMetadata;
import org.skyve.impl.metadata.repository.view.access.ViewUserAccessesMetaData;
import org.skyve.impl.metadata.view.component.Component;
import org.skyve.impl.metadata.view.model.ModelMetaData;
import org.skyve.impl.metadata.view.model.chart.ChartBuilderMetaData;
import org.skyve.impl.metadata.view.widget.Chart;
import org.skyve.impl.metadata.view.widget.MapDisplay;
import org.skyve.impl.metadata.view.widget.bound.ZoomIn;
import org.skyve.impl.metadata.view.widget.bound.input.CompleteType;
import org.skyve.impl.metadata.view.widget.bound.input.LookupDescription;
import org.skyve.impl.metadata.view.widget.bound.input.TextField;
import org.skyve.impl.metadata.view.widget.bound.tabular.DataGrid;
import org.skyve.impl.metadata.view.widget.bound.tabular.ListGrid;
import org.skyve.impl.metadata.view.widget.bound.tabular.ListRepeater;
import org.skyve.impl.metadata.view.widget.bound.tabular.TreeGrid;
import org.skyve.impl.util.UtilImpl;
import org.skyve.metadata.MetaDataException;
import org.skyve.metadata.customer.Customer;
import org.skyve.metadata.model.document.Document;
import org.skyve.metadata.model.document.Reference;
import org.skyve.metadata.model.document.Relation;
import org.skyve.metadata.module.Module;
import org.skyve.metadata.module.Module.DocumentRef;
import org.skyve.metadata.module.query.MetaDataQueryDefinition;
import org.skyve.metadata.user.UserAccess;
import org.skyve.metadata.view.Action;
import org.skyve.metadata.view.View;
import org.skyve.metadata.view.model.list.ListModel;
import org.skyve.util.Binder.TargetMetaData;

public class ViewImpl extends Container implements View {
	private static final long serialVersionUID = -2621201277538515637L;

	private String name;
	private long lastModifiedMillis = Long.MAX_VALUE;
	private String icon32x32RelativeFileName;
	private String iconStyleClass;
	private String helpRelativeFileName;
	private String helpURL;
	private String title;
	private String actionsWidgetId;
	private LinkedHashMap<String, Action> actions = new LinkedHashMap<>();
	private Integer refreshTimeInSeconds;
	private String refreshConditionName;
	private String refreshActionName;
	private List<ViewParameter> parameters = new ArrayList<>();
	private String documentation;
	// map of modelName -> model metadata used to instantiate models on the server-side
	private Map<String, ModelMetaData> inlineModels = new TreeMap<>();
	private String overriddenCustomerName;
	private String overriddenUxUiName;
	private Map<String, String> properties = new TreeMap<>();
	
	@Override
	public String getRefreshConditionName() {
		return refreshConditionName;
	}

	public void setRefreshConditionName(String refreshConditionName) {
		this.refreshConditionName = refreshConditionName;
	}

	@Override
	public Integer getRefreshTimeInSeconds() {
		return refreshTimeInSeconds;
	}

	public void setRefreshTimeInSeconds(Integer refreshTimeInSeconds) {
		this.refreshTimeInSeconds = refreshTimeInSeconds;
	}

	@Override
	public String getRefreshActionName() {
		return refreshActionName;
	}

	public void setRefreshActionName(String refreshActionName) {
		this.refreshActionName = refreshActionName;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public long getLastModifiedMillis() {
		return lastModifiedMillis;
	}
	
	public void setLastModifiedMillis(long lastModifiedMillis) {
		this.lastModifiedMillis = lastModifiedMillis;
	}
	
	@Override
	public String getIcon32x32RelativeFileName() {
		return icon32x32RelativeFileName;
	}

	public void setIcon32x32RelativeFileName(String icon32x32RelativeFileName) {
		this.icon32x32RelativeFileName = icon32x32RelativeFileName;
	}

	@Override
	public String getIconStyleClass() {
		return iconStyleClass;
	}

	public void setIconStyleClass(String iconStyleClass) {
		this.iconStyleClass = iconStyleClass;
	}

	@Override
	public String getHelpRelativeFileName() {
		return helpRelativeFileName;
	}

	public void setHelpRelativeFileName(String helpRelativeFileName) {
		this.helpRelativeFileName = helpRelativeFileName;
	}

	@Override
	public String getHelpURL() {
		return helpURL;
	}

	public void setHelpURL(String helpURL) {
		this.helpURL = helpURL;
	}

	@Override
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String getActionsWidgetId() {
		return actionsWidgetId;
	}

	public void setActionsWidgetId(String actionsWidgetId) {
		this.actionsWidgetId = actionsWidgetId;
	}

	@Override
	public Action getAction(String actionName) {
		return actions.get(actionName);
	}

	public void putAction(Action action) {
		actions.put(action.getName(), action);
	}

	@Override
	public Collection<Action> getActions() {
		return actions.values();
	}

	/**
	 * These represent parameters that are allowed to be populated when creating a new record.
	 */
	@Override
	public List<ViewParameter> getParameters() {
		return parameters;
	}

	@Override
	public String getDocumentation() {
		return documentation;
	}

	public void setDocumentation(String documentation) {
		this.documentation = documentation;
	}
	
	@Override
	public ModelMetaData getInlineModel(String modelName) {
		return inlineModels.get(modelName);
	}
	
	@Override
	public String getOverriddenCustomerName() {
		return overriddenCustomerName;
	}

	public void setOverriddenCustomerName(String overriddenCustomerName) {
		this.overriddenCustomerName = overriddenCustomerName;
	}

	@Override
	public String getOverriddenUxUiName() {
		return overriddenUxUiName;
	}

	public void setOverriddenUxUiName(String overriddenUxUiName) {
		this.overriddenUxUiName = overriddenUxUiName;
	}

	@Override
	public Map<String, String> getProperties() {
		return properties;
	}

	// Accesses for this view only (not component fragments)
	private Set<UserAccess> accesses = null;
	
	/**
	 * Get accesses for this view and any component fragments recursively.
	 */
	public Set<UserAccess> getAccesses(CustomerImpl customer, String uxui) {
		Set<UserAccess> result = accesses;
		
		if (result != null) { // accesses were generated
			for (Component component : components) {
				ViewImpl fragment = component.getFragment(customer, uxui);
				result.addAll(fragment.getAccesses(customer, uxui));
			}
		}

		return result;
	}

	public void convertAccesses(Module module,
									String documentName,
									String metaDataName,
									ViewUserAccessesMetaData accessesMetaData) {
		final String moduleName = module.getName();
		
		// Populate User Accesses
		if (accessesMetaData != null) {
			accesses = new TreeSet<>();

			for (ViewUserAccessMetaData accessMetaData : accessesMetaData.getAccesses()) {
				// Validate access metadata
				accessMetaData.validate(metaDataName, module);

				// Validate and add ux/uis
				Set<String> uxuis = null;
				List<ViewUserAccessUxUiMetadata> uxuisMetaData = accessMetaData.getUxuis();
				if (uxuisMetaData.isEmpty()) {
					uxuis = UserAccess.ALL_UX_UIS;
				}
				else {
					uxuis = new TreeSet<>();
					for (ViewUserAccessUxUiMetadata uxuiMetaData : uxuisMetaData) {
						String uxuiName = uxuiMetaData.getName();
						if (uxuiName == null) {
							throw new MetaDataException(metaDataName + " : [name] is required for UX/UI in user access " + accessMetaData.toUserAccess(moduleName, documentName).toString() + " in view");
						}
						if (! uxuis.add(uxuiMetaData.getName())) {
							throw new MetaDataException(metaDataName + " : Duplicate UX/UI of " + uxuiMetaData.getName() + " in user access " + accessMetaData.toUserAccess(moduleName, documentName).toString() + " in view");
						}
					}
				}

				// Put into accesses
				if (! accesses.add(accessMetaData.toUserAccess(moduleName, documentName))) {
					throw new MetaDataException(metaDataName + " : Duplicate user access " + accessMetaData.toUserAccess(moduleName, documentName).toString() + " in view");
				}
			}
		}
	}
	
	/**
	 * Ensure that any component loaded matches the given UX/UI.
	 * Ensure that inlined metadata model definitions are added to the implicit models map.
	 * Convert and validate any accesses defined in the view metadata.
	 * If accesses are not defined then determine them.
	 */
	public void resolve(String uxui, Customer customer, Module module, Document document, boolean generate) {
		final String moduleName = module.getName();
		final String documentName = document.getName();

		// If there are no accesses defined in view metadata, or the view says to generate them, and access control is turned on, determine them
		boolean determineAccesses = UtilImpl.ACCESS_CONTROL && generate;
		if (determineAccesses && (accesses == null)) {
			accesses = new TreeSet<>();
		}

		new NoOpViewVisitor((CustomerImpl) customer, (ModuleImpl) module, (DocumentImpl) document, this, uxui) {
			@Override
			public void visitComponent(Component component, boolean parentVisible, boolean parentEnabled) {
				// Overrides visitComponent standard behaviour to link to the component
				component.link(currentUxUi, customer, module, document, name);
				components.add(component);

				// stop recursing through the component as we only want immediate (not recursive) accesses for this view
			}
			
			@Override
			public void visitChart(Chart chart, boolean parentVisible, boolean parentEnabled) {
				// Add any inlined models to the view
				ChartBuilderMetaData metaDataModel = chart.getModel();
				if (metaDataModel != null) {
					inlineModels.put(metaDataModel.getModelName(), metaDataModel);
				}

				if (determineAccesses) {
					if (metaDataModel != null) {
						String modelModuleName = metaDataModel.getModuleName();
						String modelQueryName = metaDataModel.getQueryName();
						if (modelQueryName != null) {
							accesses.add(UserAccess.queryAggregate(modelModuleName, modelQueryName));
						}
						else {
							Module modelModule = customer.getModule(modelModuleName);
							String moduleDocumentName = metaDataModel.getDocumentName();
							DocumentRef ref = modelModule.getDocumentRefs().get(documentName);
							modelQueryName = ref.getDefaultQueryName();
							if (modelQueryName != null) {
								accesses.add(UserAccess.queryAggregate(modelModuleName, modelQueryName));
							}
							else {
								accesses.add(UserAccess.documentAggregate(modelModuleName, moduleDocumentName));
							}
						}
					}
					else {
						String modelName = chart.getModelName();
						accesses.add(UserAccess.modelAggregate(moduleName, documentName, modelName));
					}
				}
			}

			private String dataGridBinding = null;
			
			@Override
			public void visitDataGrid(DataGrid grid, boolean parentVisible, boolean parentEnabled) {
				if (determineAccesses) {
					dataGridBinding = grid.getBinding();
					if (! (Boolean.FALSE.equals(grid.getShowAdd()) && Boolean.FALSE.equals(grid.getShowZoom()))) {
						accessThroughBinding(dataGridBinding);
					}
				}
			}

			@Override
			public void visitedDataGrid(DataGrid grid, boolean parentVisible, boolean parentEnabled) {
				if (determineAccesses) {
					dataGridBinding = null;
				}
			}
			
			// NB DataRepeater cannot zoom in

			@Override
			public void visitListGrid(ListGrid grid, boolean parentVisible, boolean parentEnabled) {
				if (determineAccesses) {
					String modelName = grid.getModelName();
					String queryName = grid.getQueryName();
					if (modelName != null) {
						accesses.add(UserAccess.modelAggregate(moduleName, documentName, modelName));
					}
					else {
						accesses.add(UserAccess.queryAggregate(moduleName, queryName));
					}
	
					if (! (Boolean.FALSE.equals(grid.getShowAdd()) && Boolean.FALSE.equals(grid.getShowZoom()))) {
						Document drivingDocument = null;
						String drivingModuleName = null;
						String drivingDocumentName = null;
						
						if (modelName != null) {
							ListModel<Bean> model = document.getListModel(customer, modelName, true);
							drivingDocument = model.getDrivingDocument();
							drivingModuleName = drivingDocument.getOwningModuleName();
							drivingDocumentName = drivingDocument.getName();
						}
						else {
							MetaDataQueryDefinition query = module.getMetaDataQuery(queryName);
							drivingDocumentName = query.getDocumentName();
							Module drivingModule = query.getDocumentModule(customer);
							drivingModuleName = drivingModule.getName();
							drivingDocument = drivingModule.getDocument(customer, drivingDocumentName);
						}
	
						accesses.add(UserAccess.singular(drivingModuleName, drivingDocumentName));
					}
				}
			}
			
			@Override
			public void visitListRepeater(ListRepeater repeater, boolean parentVisible, boolean parentEnabled) {
				if (determineAccesses) {
					String modelName = repeater.getModelName();
					if (modelName != null) {
						accesses.add(UserAccess.modelAggregate(moduleName, documentName, modelName));
					}
					else {
						String queryName = repeater.getQueryName();
						accesses.add(UserAccess.queryAggregate(moduleName, queryName));
					}
					// NB ListRepeater cannot zoom in
				}
			}
			
			@Override
			public void visitLookupDescription(LookupDescription lookup, boolean parentVisible, boolean parentEnabled) {
				if (determineAccesses) {
					String binding = lookup.getBinding();
					if (dataGridBinding != null) {
						if (binding == null) { // binding can be null when placed in a data grid
							binding = dataGridBinding;
						}
						else {
							StringBuilder sb = new StringBuilder(dataGridBinding.length() + 1 + binding.length());
							sb.append(dataGridBinding).append('.').append(binding);
							binding = sb.toString();
						}
					}
					
					TargetMetaData target = BindUtil.getMetaDataForBinding(customer, module, document, binding);
					Relation targetRelation = (Relation) target.getAttribute();
					String targetDocumentName = targetRelation.getDocumentName();
	
					// Check lookup query
					String queryName = lookup.getQuery();
					// Maybe the relation is a reference and has a query
					if (queryName == null) {
						if (targetRelation instanceof Reference) {
							queryName = ((Reference) targetRelation).getQueryName();
						}
					}
					// Look for the default query for the relation document name
					if (queryName == null) {
						DocumentRef ref = module.getDocumentRefs().get(targetDocumentName);
						queryName = ref.getDefaultQueryName();
					}
	
					// add the query aggregate or a document aggregate
					if (queryName == null) {
						accesses.add(UserAccess.documentAggregate(moduleName, targetDocumentName));
					}
					else {
						accesses.add(UserAccess.queryAggregate(moduleName, queryName));
					}
	
					// If not in a grid and is editable, add the zoom in access
					if ((dataGridBinding == null) && (! Boolean.FALSE.equals(lookup.getEditable()))) {
						Document targetDocument = module.getDocument(customer, targetDocumentName);
						String targetModuleName = targetDocument.getOwningModuleName();
						accesses.add(UserAccess.singular(targetModuleName, targetDocumentName));
					}
				}
			}
			
			@Override
			public void visitMap(MapDisplay map, boolean parentVisible, boolean parentEnabled) {
				if (determineAccesses) {
					String modelName = map.getModelName();
					accesses.add(UserAccess.modelAggregate(moduleName, documentName, modelName));
	
					// NB Can't work out what the map can navigate to - needs to be added to the router manually.
				}
			}
			
			@Override
			public void visitTextField(TextField text, boolean parentVisible, boolean parentEnabled) {
				if (determineAccesses) {
					CompleteType type = text.getComplete();
					if (type == CompleteType.previous) {
						String binding = text.getBinding();
						if (dataGridBinding != null) {
							StringBuilder sb = new StringBuilder(dataGridBinding.length() + 1 + binding.length());
							sb.append(dataGridBinding).append('.').append(binding);
							binding = sb.toString();
						}
						accesses.add(UserAccess.previousComplete(moduleName, documentName, binding));
					}
				}
			}
			
			@Override
			public void visitTreeGrid(TreeGrid grid, boolean parentVisible, boolean parentEnabled) {
				if (determineAccesses) {
					visitListGrid(grid, parentVisible, parentEnabled);
				}
			}
			
			@Override
			public void visitZoomIn(ZoomIn zoomIn, boolean parentVisible, boolean parentEnabled) {
				if (determineAccesses) {
					accessThroughBinding(zoomIn.getBinding());
				}
			}

			private void accessThroughBinding(String binding) {
				TargetMetaData target = BindUtil.getMetaDataForBinding(customer, module, document, binding);

				Document relatedDocument = null;
				if (ChildBean.PARENT_NAME.equals(binding) || binding.endsWith(ChildBean.CHILD_PARENT_NAME_SUFFIX)) {
					relatedDocument = target.getDocument().getParentDocument(customer);
				}
				else {
					Relation targetRelation = (Relation) target.getAttribute();
					String relatedDocumentName = targetRelation.getDocumentName();
					relatedDocument = module.getDocument(customer, relatedDocumentName);
				}
				@SuppressWarnings("null")
				String relatedModuleName = relatedDocument.getOwningModuleName();
				accesses.add(UserAccess.singular(relatedModuleName, relatedDocument.getName()));
			}
		}.visit();
	}
	
	// Not required to be Serialized as ViewImpls are cloned by serialization to populate this
	private transient ComponentFragments fragments = new ComponentFragments(this);
	
	// All components in this view (one level deep)
	// Not required to be Serialized as ViewImpls are cloned by serialization to populate this
	private transient Set<Component> components = new HashSet<>();
	
	/**
	 * Reinstate transients after Deserialization
	 * @return	this
	 */
	private Object readResolve() {
		fragments = new ComponentFragments(this);
		components = new HashSet<>();
		return this;
	}
	
	/**
	 * Get a UI fragment for a component
	 * @param c	The customer
	 * @param m	The module
	 * @param d	The document
	 * @param uxui	The UX/UI
	 * @param component	The component
	 * @return	The fragment
	 */
	public ViewImpl getFragment(CustomerImpl c, ModuleImpl m, DocumentImpl d, String uxui, Component component) {
		return fragments.get(c, m, d, uxui, component, (accesses != null));
	}
}
