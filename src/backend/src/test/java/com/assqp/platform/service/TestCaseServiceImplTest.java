package com.assqp.platform.service;

import com.assqp.platform.dto.testing.TestCaseRequest;
import com.assqp.platform.dto.testing.TestCaseResponse;
import com.assqp.platform.entity.Project;
import com.assqp.platform.entity.TestCase;
import com.assqp.platform.enums.TestCaseStatus;
import com.assqp.platform.enums.TestType;
import com.assqp.platform.exception.BadRequestException;
import com.assqp.platform.repository.ProjectRepository;
import com.assqp.platform.repository.TestCaseRepository;
import com.assqp.platform.repository.TestExecutionRepository;
import com.assqp.platform.service.impl.TestCaseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestCaseServiceImplTest {

    @Mock
    private TestCaseRepository testCaseRepository;
    @Mock
    private ProjectRepository projectRepository;
    // Added when TestCaseServiceImpl.delete() was fixed to cascade-delete a
    // test case's executions first (Phase 12 audit) - the constructor now
    // requires this; without a mock here, @InjectMocks would pass null and
    // delete_existingTestCase_deletesSuccessfully would NPE.
    @Mock
    private TestExecutionRepository testExecutionRepository;

    @InjectMocks
    private TestCaseServiceImpl testCaseService;

    @Test
    void create_validRequest_savesTestCase() {
        Project project = Project.builder().id(1L).name("ASSQP").build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(testCaseRepository.save(any(TestCase.class))).thenAnswer(inv -> {
            TestCase tc = inv.getArgument(0);
            tc.setId(100L);
            return tc;
        });

        TestCaseRequest request = TestCaseRequest.builder()
                .title("Login test").testType("FUNCTIONAL").priority("HIGH").build();

        TestCaseResponse response = testCaseService.create(1L, request);

        assertThat(response.getTitle()).isEqualTo("Login test");
        assertThat(response.getTestType()).isEqualTo("FUNCTIONAL");
    }

    @Test
    void create_invalidTestType_throwsBadRequest() {
        Project project = Project.builder().id(1L).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        TestCaseRequest request = TestCaseRequest.builder().title("Bad").testType("NOT_REAL").build();

        assertThatThrownBy(() -> testCaseService.create(1L, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateStatus_validStatus_updatesTestCase() {
        TestCase testCase = TestCase.builder().id(5L).title("t").testType(TestType.UNIT)
                .status(TestCaseStatus.ACTIVE).project(Project.builder().id(1L).build()).build();
        when(testCaseRepository.findById(5L)).thenReturn(Optional.of(testCase));
        when(testCaseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TestCaseResponse response = testCaseService.updateStatus(5L, "DEPRECATED");

        assertThat(response.getStatus()).isEqualTo("DEPRECATED");
    }

    @Test
    void getByProjectFiltered_filtersbyType() {
        Project project = Project.builder().id(1L).build();
        TestCase unitTest = TestCase.builder().id(1L).title("Unit").testType(TestType.UNIT)
                .status(TestCaseStatus.ACTIVE).project(project).build();
        TestCase securityTest = TestCase.builder().id(2L).title("Sec").testType(TestType.SECURITY)
                .status(TestCaseStatus.ACTIVE).project(project).build();

        when(testCaseRepository.findByProjectId(1L)).thenReturn(List.of(unitTest, securityTest));

        Pageable pageable = PageRequest.of(0, 10);
        var page = testCaseService.getByProjectFiltered(1L, "SECURITY", null, null, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTestType()).isEqualTo("SECURITY");
    }

    @Test
    void delete_existingTestCase_deletesSuccessfully() {
        TestCase testCase = TestCase.builder().id(5L).testType(TestType.UNIT)
                .status(TestCaseStatus.ACTIVE).project(Project.builder().id(1L).build()).build();
        when(testCaseRepository.findById(5L)).thenReturn(Optional.of(testCase));

        testCaseService.delete(5L);

        // Phase 12 audit fix: executions must be cleared first, or a test
        // case with any execution history would fail on the FK constraint.
        verify(testExecutionRepository).deleteByTestCaseId(5L);
        verify(testCaseRepository).delete(testCase);
    }
}
