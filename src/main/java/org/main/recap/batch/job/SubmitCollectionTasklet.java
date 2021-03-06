package org.main.recap.batch.job;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.main.recap.RecapConstants;
import org.main.recap.batch.service.SubmitCollectionService;
import org.main.recap.batch.service.UpdateJobDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;

/**
 * Created by harikrishnanv on 19/6/17.
 */
public class SubmitCollectionTasklet implements Tasklet {

    private static final Logger logger = LoggerFactory.getLogger(SubmitCollectionTasklet.class);

    @Value("${scsb.circ.url}")
    String scsbCircUrl;

    @Value("${scsb.solr.client.url}")
    String solrClientUrl;

    @Autowired
    private SubmitCollectionService submitCollectionService;

    @Autowired
    private UpdateJobDetailsService updateJobDetailsService;

    /**
     * This method starts the execution of the submit collection job.
     * @param contribution
     * @param chunkContext
     * @return
     * @throws Exception
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        logger.info("Executing submit collection");
        StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext executionContext = jobExecution.getExecutionContext();
        try {
            long jobInstanceId = jobExecution.getJobInstance().getInstanceId();
            String jobName = chunkContext.getStepContext().getStepExecution().getJobExecution().getJobInstance().getJobName();
            Date createdDate = chunkContext.getStepContext().getStepExecution().getJobExecution().getCreateTime();
            updateJobDetailsService.updateJob(solrClientUrl, jobName, createdDate, jobInstanceId);
            String resultStatus = submitCollectionService.submitCollection(scsbCircUrl);
            logger.info("Submit Collection status : {}", resultStatus);
            if (StringUtils.containsIgnoreCase(resultStatus, RecapConstants.FAIL)) {
                executionContext.put(RecapConstants.JOB_STATUS, RecapConstants.FAILURE);
                executionContext.put(RecapConstants.JOB_STATUS_MESSAGE, RecapConstants.SUBMIT_COLLECTION_STATUS_NAME + " " + resultStatus);
                stepExecution.setExitStatus(new ExitStatus(RecapConstants.FAILURE,  RecapConstants.SUBMIT_COLLECTION_STATUS_NAME + " " + resultStatus));
            } else {
                executionContext.put(RecapConstants.JOB_STATUS, RecapConstants.SUCCESS);
                executionContext.put(RecapConstants.JOB_STATUS_MESSAGE,  RecapConstants.SUBMIT_COLLECTION_STATUS_NAME + " " + resultStatus);
                stepExecution.setExitStatus(new ExitStatus(RecapConstants.SUCCESS,  RecapConstants.SUBMIT_COLLECTION_STATUS_NAME + " " + resultStatus));
            }
        } catch (Exception ex) {
            logger.error(RecapConstants.LOG_ERROR, ExceptionUtils.getMessage(ex));
            executionContext.put(RecapConstants.JOB_STATUS, RecapConstants.FAILURE);
            executionContext.put(RecapConstants.JOB_STATUS_MESSAGE, RecapConstants.SUBMIT_COLLECTION_STATUS_NAME + " " + ExceptionUtils.getMessage(ex));
            stepExecution.setExitStatus(new ExitStatus(RecapConstants.FAILURE, ExceptionUtils.getFullStackTrace(ex)));
        }
        return RepeatStatus.FINISHED;
    }
}
