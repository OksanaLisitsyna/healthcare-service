import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import ru.netology.patient.entity.BloodPressure;
import ru.netology.patient.entity.HealthInfo;
import ru.netology.patient.entity.PatientInfo;
import ru.netology.patient.repository.PatientInfoRepository;
import ru.netology.patient.service.alert.SendAlertService;
import ru.netology.patient.service.medical.MedicalService;
import ru.netology.patient.service.medical.MedicalServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

public class MedicalServiceImplTest {
    private static PatientInfoRepository patientInfoRepository;

    //создадим заглушку для patientInfoRepository
    //человека с нормальным давлением и температурой
    @BeforeAll
    public static void makeMockForPatientRepository() {
        patientInfoRepository = Mockito.mock(PatientInfoRepository.class);
        Mockito.when(patientInfoRepository.getById(Mockito.any()))
                .thenReturn(new PatientInfo("Иван", "Иванов", LocalDate.of(1980, 11, 26),
                        new HealthInfo(new BigDecimal("36.6"), new BloodPressure(120, 80))));
    }

    // набор аргументов для проверки давления
    public static Stream<Arguments> methodSourceForBloodPressure() {
        return Stream.of(
                Arguments.of(new BloodPressure(120, 80), 0), // давление в норме, send не вызывается
                Arguments.of(new BloodPressure(130, 90), 1), // давление не в норме(оба показателя выше), send вызывается 1 раз
                Arguments.of(new BloodPressure(130, 80), 1), // 1 показатель выше, send вызывается 1 раз
                Arguments.of(new BloodPressure(120, 90), 1),
                Arguments.of(new BloodPressure(110, 70), 1), // оба показателя ниже, send не вызывается 1 раз
                Arguments.of(new BloodPressure(110, 80), 1), // 1 показатель ниже, send вызывается 1 раз
                Arguments.of(new BloodPressure(120, 70), 1)
        );
    }

    // набор аргументов для проверки температуры
    public static Stream<Arguments> methodSourceForTemperature() {
        return Stream.of(
                Arguments.of(new BigDecimal("36.6"), 0), // температура в норме, send не вызывается
                Arguments.of(new BigDecimal("36.0"), 0), // температура колеблется в пределах нормы send не зывается
                Arguments.of(new BigDecimal("38.8"), 1), // температура выше, send должен вызываться 1 раз
                Arguments.of(new BigDecimal("34.8"), 1) // температура ниже, send вызывается 1 раз
        );
    }


    //проверяем вывод сообщения, если давление НЕ в норме,
    //и отсутствие сообщения, если давление в норме
    @ParameterizedTest
    @MethodSource("methodSourceForBloodPressure")
    public void checkBloodPressureTest(BloodPressure bloodPressure, int numberOfMethodCalls) {
        SendAlertService alertService = Mockito.mock(SendAlertService.class);
        MedicalService medicalService = new MedicalServiceImpl(patientInfoRepository, alertService);
        medicalService.checkBloodPressure("id", bloodPressure);
        Mockito.verify(alertService, Mockito.times(numberOfMethodCalls)).send(Mockito.anyString());
    }


    //проверяем вывод сообщения, если температура не в норме
    //почему-то если температура выше send не вызывается??
    @ParameterizedTest
    @MethodSource("methodSourceForTemperature")
    public void checkTemperatureTest(BigDecimal temperature, int numberOfMethodCalls) {
        SendAlertService alertService = Mockito.mock(SendAlertService.class);
        MedicalService medicalService = new MedicalServiceImpl(patientInfoRepository, alertService);
        medicalService.checkTemperature("id", temperature);
        Mockito.verify(alertService, Mockito.times(numberOfMethodCalls)).send(Mockito.anyString());
    }
}
