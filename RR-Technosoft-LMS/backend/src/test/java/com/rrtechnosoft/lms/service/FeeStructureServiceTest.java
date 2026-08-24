package com.rrtechnosoft.lms.service;

import com.rrtechnosoft.lms.dto.request.CreateFeeStructureRequest;
import com.rrtechnosoft.lms.dto.request.FeeInstallmentInput;
import com.rrtechnosoft.lms.dto.request.UpdateFeeStructureRequest;
import com.rrtechnosoft.lms.entity.FeeStructure;
import com.rrtechnosoft.lms.exception.ApiException;
import com.rrtechnosoft.lms.repository.CourseRepository;
import com.rrtechnosoft.lms.repository.FeeStructureRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeStructureServiceTest {

    @Mock private FeeStructureRepository feeStructureRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private FeeStructureService feeStructureService;

    private final UUID actorId = UUID.randomUUID();

    // ------------------------------------------------------- create

    @Test
    void create_defaultsCurrencyToInrWhenBlank() {
        CreateFeeStructureRequest request = new CreateFeeStructureRequest(
                null, "Full course fee", "desc", new BigDecimal("30000"), "  ",
                List.of(new FeeInstallmentInput(1, new BigDecimal("30000"), 0))
        );
        when(feeStructureRepository.save(any(FeeStructure.class))).thenAnswer(inv -> {
            FeeStructure s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        var response = feeStructureService.create(request, actorId);

        assertThat(response.currency()).isEqualTo("INR");
        assertThat(response.installmentCount()).isEqualTo(1);
        verify(auditLogService).log(actorId, "CREATE_FEE_STRUCTURE", "FeeStructure", response.id(), null);
    }

    @Test
    void create_preservesExplicitCurrency() {
        CreateFeeStructureRequest request = new CreateFeeStructureRequest(
                null, "Full course fee", "desc", new BigDecimal("500"), "USD",
                List.of(new FeeInstallmentInput(1, new BigDecimal("500"), 0))
        );
        when(feeStructureRepository.save(any(FeeStructure.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = feeStructureService.create(request, actorId);

        assertThat(response.currency()).isEqualTo("USD");
    }

    @Test
    void create_unknownCourse_throwsNotFound() {
        UUID courseId = UUID.randomUUID();
        CreateFeeStructureRequest request = new CreateFeeStructureRequest(
                courseId, "Course fee", null, new BigDecimal("100"), null,
                List.of(new FeeInstallmentInput(1, new BigDecimal("100"), 0))
        );
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feeStructureService.create(request, actorId))
                .isInstanceOf(ApiException.class)
                .hasMessage("Course not found");
    }

    // ------------------------------------------- installment validation

    @Test
    void create_installmentsMustSumToTotal() {
        CreateFeeStructureRequest request = new CreateFeeStructureRequest(
                null, "Course fee", null, new BigDecimal("1000"), null,
                List.of(new FeeInstallmentInput(1, new BigDecimal("400"), 0),
                        new FeeInstallmentInput(2, new BigDecimal("500"), 30))
        );

        assertThatThrownBy(() -> feeStructureService.create(request, actorId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("must add up to the total amount");
    }

    @Test
    void create_installmentNumbersMustBeSequentialStartingAtOne() {
        CreateFeeStructureRequest request = new CreateFeeStructureRequest(
                null, "Course fee", null, new BigDecimal("1000"), null,
                List.of(new FeeInstallmentInput(1, new BigDecimal("500"), 0),
                        new FeeInstallmentInput(3, new BigDecimal("500"), 30))
        );

        assertThatThrownBy(() -> feeStructureService.create(request, actorId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("sequential starting at 1");
    }

    @Test
    void create_emptyInstallmentList_throwsBadRequest() {
        CreateFeeStructureRequest request = new CreateFeeStructureRequest(
                null, "Course fee", null, new BigDecimal("1000"), null, List.of()
        );

        assertThatThrownBy(() -> feeStructureService.create(request, actorId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("At least one installment is required");
    }

    @Test
    void create_installmentsOutOfOrderButStillSequential_isAccepted() {
        // installment numbers don't need to arrive sorted, only be {1..n} once sorted
        CreateFeeStructureRequest request = new CreateFeeStructureRequest(
                null, "Course fee", null, new BigDecimal("1000"), null,
                List.of(new FeeInstallmentInput(2, new BigDecimal("500"), 30),
                        new FeeInstallmentInput(1, new BigDecimal("500"), 0))
        );
        when(feeStructureRepository.save(any(FeeStructure.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = feeStructureService.create(request, actorId);

        assertThat(response.installmentCount()).isEqualTo(2);
    }

    // ------------------------------------------------------- update

    @Test
    void update_blankNameIsIgnored_descriptionAndActiveStillApply() {
        UUID id = UUID.randomUUID();
        FeeStructure existing = FeeStructure.builder().id(id).name("Original").isActive(true).build();
        when(feeStructureRepository.findWithInstallmentsById(id)).thenReturn(Optional.of(existing));
        when(feeStructureRepository.save(any(FeeStructure.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = feeStructureService.update(id, new UpdateFeeStructureRequest("  ", "New desc", false), actorId);

        assertThat(response.name()).isEqualTo("Original");
        assertThat(response.description()).isEqualTo("New desc");
        assertThat(response.isActive()).isFalse();
    }

    @Test
    void update_unknownId_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(feeStructureRepository.findWithInstallmentsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feeStructureService.update(id, new UpdateFeeStructureRequest("X", null, null), actorId))
                .isInstanceOf(ApiException.class)
                .hasMessage("Fee structure not found");
    }

    // ------------------------------------------------------- delete (soft)

    @Test
    void delete_deactivatesRatherThanRemoving() {
        UUID id = UUID.randomUUID();
        FeeStructure existing = FeeStructure.builder().id(id).name("X").isActive(true).build();
        when(feeStructureRepository.findWithInstallmentsById(id)).thenReturn(Optional.of(existing));
        when(feeStructureRepository.save(any(FeeStructure.class))).thenAnswer(inv -> inv.getArgument(0));

        feeStructureService.delete(id, actorId);

        assertThat(existing.getIsActive()).isFalse();
        verify(auditLogService).log(actorId, "DEACTIVATE_FEE_STRUCTURE", "FeeStructure", id, null);
    }
}
