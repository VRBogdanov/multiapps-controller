package org.cloudfoundry.multiapps.controller.process.flowable;

import java.util.List;
import java.util.ListIterator;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.model.HistoricOperationEvent.EventType;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.HistoricOperationEventPersister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class RetryProcessAction extends ProcessAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryProcessAction.class);

    public static final String ACTION_ID_RETRY = "retry";

    private final HistoricOperationEventPersister historicOperationEventPersister;

    @Inject
    public RetryProcessAction(FlowableFacade flowableFacade, List<AdditionalProcessAction> additionalProcessActions,
                              HistoricOperationEventPersister historicOperationEventPersister,
                              CloudControllerClientProvider cloudControllerClientProvider) {
        super(flowableFacade, additionalProcessActions, cloudControllerClientProvider);
        this.historicOperationEventPersister = historicOperationEventPersister;
    }

    @Override
    protected void executeActualProcessAction(String user, String superProcessInstanceId) {
        List<String> subProcessIds = getActiveExecutionIds(superProcessInstanceId);
        ListIterator<String> subProcessesIdsIterator = subProcessIds.listIterator(subProcessIds.size());

        updateUserIfNecessary(user, superProcessInstanceId);
        while (subProcessesIdsIterator.hasPrevious()) {
            String subProcessId = subProcessesIdsIterator.previous();
            retryProcess(subProcessId);
        }
        historicOperationEventPersister.add(superProcessInstanceId, EventType.RETRIED);
    }

    private void retryProcess(String subProcessId) {
        try {
            flowableFacade.executeJob(subProcessId);
        } catch (RuntimeException e) {
            // Consider the retry as successful. The execution error could be later obtained through
            // the getError() method.
            LOGGER.error(Messages.FLOWABLE_JOB_RETRY_FAILED, e);
        }
    }

    @Override
    public String getActionId() {
        return ACTION_ID_RETRY;
    }

}
