package org.sigmah.client.util.profiler;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

/**
 * Export the content of the execution table inside a Markdown report.
 * 
 * @author Raphaël Calabro (raphael.calabro@netapsys.fr)
 */
public class FullReportBuilder {
	
	@Inject
	private ExecutionAsyncDAO executionAsyncDAO;
	
	private final StringBuilder stringBuilder = new StringBuilder();
	
	private void buildReport(final AsyncCallback<String> callback) {
		
		appendHeader();
		
		executionAsyncDAO.forEach(new AsyncCallback<Execution>() {
			@Override
			public void onFailure(Throwable caught) {
				callback.onFailure(caught);
			}

			@Override
			public void onSuccess(Execution result) {
				if (result != null) {
					appendExecution(result);
				} else {
					// On end
				}
			}
		});
	}
	
	private void appendHeader() {
		stringBuilder.append("# Sigmah Performance Report\n\n"
				+ "^ Scenario  ^ ");
	}
	
	private void appendExecution(Execution execution) {
		stringBuilder.append("# Sigmah Performance Report\n\n");
	}
	
}
