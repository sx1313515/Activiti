package org.activiti.spring.boot.process;

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.spring.boot.security.util.SecurityUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class ProcessRuntimeCallActivityMappingIT {

    private static final String PARENT_PROCESS_CALL_ACTIVITY = "parentproc-843144bc-3797-40db-8edc-d23190b118e5";
    private static final String SUB_PROCESS_CALL_ACTIVITY = "subprocess-fb5f2386-709a-4947-9aa0-bbf31497384g";
    
    @Autowired
    private ProcessRuntime processRuntime;

    @Autowired
    private TaskRuntime taskRuntime;

    @Autowired
    private SecurityUtil securityUtil;

    @Test
    public void testCheckSubProcessTaskWhenCallActivity (){

        securityUtil.logInAs("salaboy");

        // After the process has started, the subProcess task should be active
        ProcessInstance processInstance = processRuntime.start(
                ProcessPayloadBuilder
                        .start()
                        .withProcessDefinitionKey(PARENT_PROCESS_CALL_ACTIVITY)
                        .build());

        assertThat(processInstance).isNotNull();

        //verify the existence of the sub process itself
        List<ProcessInstance> subProcessInstanceList = processRuntime.processInstances(
                Pageable.of(0, 50),
                ProcessPayloadBuilder
                        .processInstances()
                        .withParentProcessInstanceId(processInstance.getId())
                        .build())
                .getContent();

        assertThat(subProcessInstanceList).isNotEmpty();

        ProcessInstance subProcessInstance = subProcessInstanceList.get(0);

        assertThat(subProcessInstance).isNotNull();
        assertThat(subProcessInstance.getParentId()).isEqualTo(processInstance.getId());
        assertThat(subProcessInstance.getProcessDefinitionKey()).isEqualTo(SUB_PROCESS_CALL_ACTIVITY);

        //verify the existence of the task in the sub process
        List <Task> taskList = taskRuntime.tasks(
                Pageable.of(0, 50),
                TaskPayloadBuilder
                        .tasks()
                        .withProcessInstanceId(subProcessInstance.getId())
                        .build())
                .getContent();

        assertThat(taskList).isNotEmpty();

        //parent process
        List<VariableInstance> procVariables = processRuntime.variables( ProcessPayloadBuilder
                .variables()
                .withProcessInstanceId(processInstance.getId())
                .build());

        for (VariableInstance procVariable : procVariables) {
            System.out.println("ParentProcess:"+procVariable.getName()+":"+procVariable.getValue());

        }

       //subprocess
        List<VariableInstance> subProcVariables = processRuntime.variables( ProcessPayloadBuilder
                .variables()
                .withProcessInstanceId(subProcessInstance.getId())
                .build());

        for (VariableInstance procVariable : subProcVariables) {
            System.out.println("Subprocess:"+procVariable.getName()+":"+procVariable.getValue());

        }


        assertThat(subProcVariables).extracting(VariableInstance::getName,
                VariableInstance::getValue)
                .containsOnly(
                        tuple("input-variable-name-1",
                                "inName"),
                        tuple("input-variable-name-2",
                                20),
                        tuple("input-variable-name-3",
                                5),
                        tuple("input-static-value",
                                "a static value")
                );

        //parent process
        List<VariableInstance> pProcVariables = processRuntime.variables( ProcessPayloadBuilder
                .variables()
                .withProcessInstanceId(processInstance.getId())
                .build());

        for (VariableInstance procVariable : pProcVariables) {
            System.out.println("ParentProcess:"+procVariable.getName()+":"+procVariable.getValue());
        }


        Task task = taskList.get(0);

        assertThat(task).isNotNull();

        assertThat("my-task-call-activity").isEqualTo(task.getName());

        //set subprocess variables
        //out_var_name_1="fromSubprocessName"
        //out_var_name_2=39

        //finish subprocess
        //check if in main process variables are changed


        //parent process
        List<VariableInstance> parentVariables = processRuntime.variables( ProcessPayloadBuilder
                .variables()
                .withProcessInstanceId(processInstance.getId())
                .build());

        for (VariableInstance procVariable : procVariables) {
            System.out.println("ParentProcess:"+procVariable.getName()+":"+procVariable.getValue());
            assertThat(subProcVariables).extracting(VariableInstance::getName,
                    VariableInstance::getValue)
                    .containsOnly(
                            tuple("out-var-name-1",
                                    "fromSubprocessName"),
                            tuple("out-var-name-2",
                                    39)
                    );



        }
    }

}
