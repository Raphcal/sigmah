package org.sigmah.client.page.project.pivot;

import javax.management.Notification;

import org.sigmah.client.EventBus;
import org.sigmah.client.dispatch.Dispatcher;
import org.sigmah.client.dispatch.monitor.MaskingAsyncMonitor;
import org.sigmah.client.event.IndicatorEvent;
import org.sigmah.client.event.ProjectEvent;
import org.sigmah.client.i18n.I18N;
import org.sigmah.client.page.common.dialog.FormDialogCallback;
import org.sigmah.client.page.common.toolbar.ActionListener;
import org.sigmah.client.page.common.toolbar.ActionToolBar;
import org.sigmah.client.page.common.toolbar.UIActions;
import org.sigmah.client.page.config.design.IndicatorDialog;
import org.sigmah.client.page.project.ProjectSubPresenter;
import org.sigmah.client.page.table.PivotGridCellEvent;
import org.sigmah.client.page.table.PivotGridHeaderEvent;
import org.sigmah.client.page.table.PivotGridHeaderEvent.IconTarget;
import org.sigmah.client.page.table.PivotGridPanel;
import org.sigmah.client.page.table.PivotGridPanel.PivotTableRow;
import org.sigmah.client.util.DateUtilGWTImpl;
import org.sigmah.shared.command.BatchCommand;
import org.sigmah.shared.command.GenerateElement;
import org.sigmah.shared.command.GetIndicators;
import org.sigmah.shared.command.GetProfiles;
import org.sigmah.shared.command.GetProject;
import org.sigmah.shared.command.UpdateMonthlyReports;
import org.sigmah.shared.command.result.BatchResult;
import org.sigmah.shared.command.result.IndicatorListResult;
import org.sigmah.shared.domain.element.IndicatorsListElement;
import org.sigmah.shared.dto.IndicatorDTO;
import org.sigmah.shared.dto.ProjectDTO;
import org.sigmah.shared.dto.SiteDTO;
import org.sigmah.shared.report.content.EntityCategory;
import org.sigmah.shared.report.content.MonthCategory;
import org.sigmah.shared.report.content.PivotContent;
import org.sigmah.shared.report.content.PivotTableData.Axis;
import org.sigmah.shared.report.model.DimensionType;
import org.sigmah.shared.report.model.PivotTableElement;

import com.allen_sauer.gwt.log.client.Log;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FieldEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.store.Record;
import com.extjs.gxt.ui.client.store.StoreEvent;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.form.CheckBox;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.toolbar.FillToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class ProjectPivotContainer extends ContentPanel implements ProjectSubPresenter, ActionListener {

	private final EventBus eventBus;
	private final Dispatcher dispatcher;

	private final IndicatorFilterCombo indicatorFilter;
	private final SiteFilterCombo siteFilter;
	private final DateFilterCombo dateFilter;
	private final PivotGridPanel gridPanel;
	private final Provider<IndicatorDialog> indicatorDialogProvider;

	private int currentDatabaseId;
	private HistorySelector historySelector;

	private LayoutComposer composer;

	private ActionToolBar toolBar;
	
	private boolean axesSwapped = false;
	
	private boolean historyNavigation = false;
	
	@Inject
	public ProjectPivotContainer(EventBus eventBus, Dispatcher dispatcher, PivotGridPanel gridPanel, Provider<IndicatorDialog> indicatorDialog) {
		this.dispatcher = dispatcher;
		this.eventBus = eventBus;
		this.gridPanel = gridPanel;
		this.indicatorDialogProvider = indicatorDialog;
		
		ProjectPivotResources.INSTANCE.style().ensureInjected();

		setHeaderVisible(false);
		setLayout(new FitLayout());
		gridPanel.setHeading(I18N.CONSTANTS.indicators());
		gridPanel.addListener(Events.HeaderClick, new Listener<PivotGridHeaderEvent>() {

			@Override
			public void handleEvent(PivotGridHeaderEvent be) {
				onHeaderClicked(be);
			}
		});
		gridPanel.addListener(Events.AfterEdit, new Listener<PivotGridCellEvent>() {

			@Override
			public void handleEvent(PivotGridCellEvent event) {
				onCellEdited(event);
			}
		});
		gridPanel.getStore().addListener(Events.Update, new Listener<StoreEvent<PivotGridPanel.PivotTableRow>>() {

			@Override
			public void handleEvent(StoreEvent<PivotTableRow> be) {
				toolBar.setDirty(true);
			}
		});
		add(gridPanel);

		indicatorFilter = new IndicatorFilterCombo(dispatcher);
		indicatorFilter.addListener(Events.Select, new Listener<FieldEvent>() {

			@Override
			public void handleEvent(FieldEvent event) {
				onIndicatorSelected();
			}
		});
		siteFilter = new SiteFilterCombo(dispatcher);
		siteFilter.addListener(Events.Select, new Listener<FieldEvent>() {

			@Override
			public void handleEvent(FieldEvent be) {		
				onSiteSelected();
			}
		});

		dateFilter = new DateFilterCombo();
		dateFilter.addListener(Events.Select, new Listener<BaseEvent>() {

			@Override
			public void handleEvent(BaseEvent be) {
				onDateSelected();
			}
		});

		historySelector = new HistorySelector();
		historySelector.addValueChangeHandler(new ValueChangeHandler<PivotLayout>() {

			@Override
			public void onValueChange(ValueChangeEvent<PivotLayout> event) {
				historyValueChange(event.getValue());
			}
		});

		toolBar = new ActionToolBar();
		toolBar.addStyleName(ProjectPivotResources.INSTANCE.style().toolbar());
		toolBar.addSaveSplitButton();
		toolBar.setDirty(false);
		
		toolBar.add(new SeparatorToolItem());
		toolBar.add(new Label(I18N.CONSTANTS.site()));
		toolBar.add(siteFilter);
		toolBar.add(new Label(I18N.CONSTANTS.indicator()));
		toolBar.add(indicatorFilter);
		toolBar.add(new Label(I18N.CONSTANTS.indicatorFilterToolBarLabel()));
		toolBar.add(dateFilter);

		toolBar.add(new FillToolItem());
		toolBar.add(historySelector.getPrevButton());
		toolBar.add(historySelector.getNextButton());
		toolBar.add(new Label(I18N.CONSTANTS.defaultView()));
		toolBar.add(new CheckBox());
		toolBar.setListener(this);
		toolBar.setDirty(false);
		setTopComponent(toolBar);
		
		eventBus.addListener(IndicatorEvent.CHANGED, new Listener<IndicatorEvent>() {

			@Override
			public void handleEvent(IndicatorEvent be) {
				if(be.getSource() != ProjectPivotContainer.this) {
					historyValueChange(historySelector.getValue());
				}
			}
		});
		
		eventBus.addListener(ProjectEvent.CHANGED, new Listener<ProjectEvent>() {

			@Override
			public void handleEvent(ProjectEvent event) {
				if(event.getProjectId()== currentDatabaseId) {
					onProjectChanged();
				}
			}
		});
	}


	@Override
	public void loadProject(ProjectDTO project) {
		this.currentDatabaseId = project.getId();
		indicatorFilter.setDatabaseId(currentDatabaseId);
		siteFilter.setDatabaseId(currentDatabaseId);
		dateFilter.fillMonths(project.getStartDate());
		composer = new LayoutComposer(new DateUtilGWTImpl(), project);

		dateFilter.setValue(dateFilter.getStore().getAt(0));
		onDateSelected();
	}
	
	private void onProjectChanged() {
		dispatcher.execute(new GetProject(currentDatabaseId), new MaskingAsyncMonitor(this, I18N.CONSTANTS.loading()),
				new AsyncCallback<ProjectDTO>() {

			@Override
			public void onFailure(Throwable caught) {
			}
	
			@Override
			public void onSuccess(ProjectDTO project) {
				dateFilter.fillMonths(project.getStartDate());			
			}
			
		});
	}



	private void onIndicatorSelected() {
		siteFilter.clear();
		dateFilter.clear();

		IndicatorDTO indicator = indicatorFilter.getValue();
		gridPanel.setHeading(I18N.MESSAGES.projectPivotByIndicator(indicator.getName()));
		gridPanel.setShowSwapIcon(false);

		PivotTableElement pivot = composer.fixIndicator(indicator.getId());
		loadPivot(new PivotLayout(indicator), pivot);
	}

	private void onSiteSelected() {
		indicatorFilter.clear();
		dateFilter.clear();
		SiteDTO site = (SiteDTO)siteFilter.getValue();

		PivotTableElement pivot = composer.fixSite(site.getId());
		gridPanel.setHeading(I18N.MESSAGES.projectPivotBySite(site.getLocationName()));
		gridPanel.setShowSwapIcon(false);
		
		loadPivot(new PivotLayout(site), pivot);
	}

	private void onDateSelected() {
		indicatorFilter.clear();
		siteFilter.clear();

		DateRangeModel dateRangeModel = dateFilter.getValue();
		
		gridPanel.setHeading(I18N.MESSAGES.projectPivotByMonth(dateRangeModel.getLabel()));
		gridPanel.setShowSwapIcon(true);
		
		PivotTableElement pivot = composer.fixDateRange(dateRangeModel.getDateRange(), axesSwapped);

		loadPivot(new PivotLayout(dateRangeModel), pivot);
	}

	private void onHeaderClicked(PivotGridHeaderEvent event) {
		Log.debug("Header clicked : " + event.getAxis());

		if(event.getIconTarget() == IconTarget.ZOOM) {
			onZoom(event);
		} else if(event.getIconTarget() == IconTarget.EDIT) {
			Axis axis = event.getAxis();
			if(axis.getDimension().getType() == DimensionType.Indicator) {
				editIndicator( ((EntityCategory)axis.getCategory()).getId() );
			}
		} else if(event.getIconTarget() == IconTarget.SWAP) {
			axesSwapped = !axesSwapped;
			onDateSelected();
		}
	}

	private void onZoom(PivotGridHeaderEvent event) {
		Axis axis = event.getAxis();
		if(axis.getDimension().getType() == DimensionType.Site) {
			SiteDTO site = new SiteDTO();
			site.setId( ((EntityCategory)axis.getCategory()).getId() );
			site.setLocationName( axis.getLabel() );

			siteFilter.setValue(site);
			onSiteSelected( );

		} else if(axis.getDimension().getType() == DimensionType.Indicator) {
			IndicatorDTO indicator = new IndicatorDTO();
			indicator.setId( ((EntityCategory)axis.getCategory()).getId() );
			indicator.setName( axis.getLabel() );

			indicatorFilter.setValue(indicator);
			onIndicatorSelected( );
		} else if(axis.getDimension().getType() == DimensionType.Date) {
			if(axis.getCategory() instanceof MonthCategory) {
				DateRangeModel model = DateRangeModel.monthModel((MonthCategory)axis.getCategory());
				dateFilter.setValue(model);
				onDateSelected();
			}
		}	

	}
	
	private void editIndicator(final int id) {
		dispatcher.execute(new GetIndicators(this.currentDatabaseId), new MaskingAsyncMonitor(this, I18N.CONSTANTS.loading()), new AsyncCallback<IndicatorListResult>() {

			@Override
			public void onFailure(Throwable caught) {
				// monitor
			}

			@Override
			public void onSuccess(IndicatorListResult result) {
				for(IndicatorDTO indicator : result.getData()) {
					if(indicator.getId() == id) {
						editIndicator(indicator);
					}
				}
			}
			
		});
		
	}

	protected void editIndicator(IndicatorDTO indicator) {
		IndicatorDialog dialog = indicatorDialogProvider.get();
		dialog.show(currentDatabaseId, indicator);
	
	}


	private void onCellEdited(final PivotGridCellEvent event) {
		toolBar.setDirty(true);
		if(event.getCell() != null) {
			if(event.getCell().getCount() > 1) {
				MessageBox.confirm(I18N.CONSTANTS.confirmUpdate(), I18N.CONSTANTS.confirmUpdateOfAggregatedCell(), new Listener<MessageBoxEvent>() {

					@Override
					public void handleEvent(MessageBoxEvent mbEvent) {
						if(!mbEvent.getButtonClicked().getItemId().equals(MessageBox.OK)) {
							event.getRecord().set(event.getProperty(), event.getCell().getValue());
						}
					}
				});
			}
		}
	}

	private void historyValueChange(PivotLayout value) {
		historyNavigation = true;
		if(value.getFilter() instanceof SiteDTO) {
			siteFilter.setValue(value.getFilter());
			onSiteSelected();
		} else if(value.getFilter() instanceof IndicatorDTO) {
			indicatorFilter.setValue((IndicatorDTO) value.getFilter());
			onIndicatorSelected();
		} else if(value.getFilter() instanceof DateRangeModel) {
			dateFilter.setValue((DateRangeModel) value.getFilter());
			onDateSelected();
		}
	}

	private void loadPivot(final PivotLayout layout, final PivotTableElement pivot) {
		dispatcher.execute(new GenerateElement<PivotContent>(pivot), new MaskingAsyncMonitor(this, I18N.CONSTANTS.loading()), 
				new AsyncCallback<PivotContent>() {

			@Override
			public void onFailure(Throwable caught) {
				MessageBox.alert("Pivot", "Pivot failed: " + caught.getMessage(), null);
			}

			@Override
			public void onSuccess(PivotContent content) {
				pivot.setContent(content);
				gridPanel.setValue(pivot);
				historySelector.onNewLayout(layout);
				historyNavigation = false;
			}
		});
	}


	@Override
	public void onUIAction(String actionId) {
		if(UIActions.save.equals(actionId)) {
			save();
		} else if(UIActions.discardChanges.equals(actionId)) {
			discard();
		}
	}

	private void save() {

		dispatcher.execute(gridPanel.composeSaveCommand(),
				new MaskingAsyncMonitor(this, I18N.CONSTANTS.saving()), new AsyncCallback<BatchResult>() {

			@Override
			public void onFailure(Throwable caught) {
				// handled by monitor
			}

			@Override
			public void onSuccess(BatchResult result) {
				gridPanel.getStore().commitChanges();
				toolBar.setDirty(false);
				eventBus.fireEvent(IndicatorEvent.CHANGED, new IndicatorEvent(ProjectPivotContainer.this));
			}
		});
	}
	
	private void discard() {
		gridPanel.getStore().rejectChanges();
		toolBar.setDirty(false);
	}

	@Override
	public Component getView() {
		return this;
	}

	@Override
	public void discardView() {

	}

	@Override
	public void viewDidAppear() {

	}

}
