package com.assqp.platform.service;

import com.assqp.platform.dto.testing.TestExecutionRequest;
import com.assqp.platform.dto.testing.TestExecutionResponse;
import com.assqp.platform.entity.Project;
import com.assqp.platform.entity.TestCase;
import com.assqp.platform.entity.TestExecution;
import com.assqp.platform.enums.ExecutionStatus;
import com.assqp.platform.enums.TestType;
import com.assqp.platform.repository.TestCaseRepository;
import com.assqp.platform.repository.TestExecutionRepository;
import com.assqp.platform.service.impl.TestExecutionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestExecutionServiceImplTest {

    @Mock
    private TestExecutionRepository testExecutionRepository;
    @Mock
    private TestCaseRepository testCaseRepository;

    @InjectMocks
    private TestExecutionServiceImpl testExecutionService;

    @Test
    void execute_passedStatus_savesExecutionAndUpdatesTestCase() {
        TestCase testCase = TestCase.builder().id(1L).testType(TestType.UNIT)
                .project(Project.builder().id(1L).build()).historicalFailureRate(0.0).build();

        when(testCaseRepository.findById(1L)).thenReturn(Optional.of(testCase));
        when(testExecutionRepository.save(any(TestExecution.class))).thenAnswer(inv -> {
            TestExecution e = inv.getArgument(0);
            e.setId(50L);
            return e;
        });
        when(testExecutionRepository.findByTestCaseIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        TestExecutionRequest request = TestExecutionRequest.builder().status("PASSED").result("OK").build();
        TestExecutionResponse response = testExecutionService.execute(1L, request);

        assertThat(response.getStatus()).isEqualTo("PASSED");
        verify(testCaseRepository).save(testCase);
        assertThat(testCase.getHistoricalFailureRate()).isEqualTo(0.0);
    }

    @Test
    void execute_failedStatus_increasesHistoricalFailureRate() {
        TestCase testCase = TestCase.builder().id(1L).testType(TestType.UNIT)
                .project(Project.builder().id(1L).build()).historicalFailureRate(0.0).build();

        when(testCaseRepository.findById(1L)).thenReturn(Optional.of(testCase));
        when(testExecutionRepository.save(any(TestExecution.class))).thenAnswer(inv -> inv.getArgument(0));
        when(testExecutionRepository.findByTestCaseIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        TestExecutionRequest request = TestExecutionRequest.builder().status("FAILED").errorMessage("NPE").build();
        testExecutionService.execute(1L, request);

        assertThat(testCase.getHistoricalFailureRate()).isEqualTo(1.0);
    }

    @Test
    void getByTestCase_returnsExecutionHistoryOrderedByRecency() {
        TestCase testCase = TestCase.builder().id(1L).build();
        TestExecution e1 = TestExecution.builder().id(1L).testCase(testCase).status(ExecutionStatus.PASSED).build();
        TestExecution e2 = TestExecution.builder().id(2L).testCase(testCase).status(ExecutionStatus.FAILED).build();

        when(testExecutionRepository.findByTestCaseIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(e2, e1));

        List<TestExecutionResponse> history = testExecutionService.getByTestCase(1L);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getStatus()).isEqualTo("FAILED");
    }
}
