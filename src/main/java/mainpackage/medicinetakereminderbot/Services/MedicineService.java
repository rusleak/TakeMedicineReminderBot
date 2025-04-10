package mainpackage.medicinetakereminderbot.Services;

import mainpackage.medicinetakereminderbot.Models.Medicine;
import mainpackage.medicinetakereminderbot.Repositories.MedicineRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class MedicineService {
    private final MedicineRepo medicineRepo;
    @Autowired
    public MedicineService(MedicineRepo medicineRepo) {
        this.medicineRepo = medicineRepo;
    }
    @Transactional
    public void save(Medicine medicine){
        medicineRepo.save(medicine);
    }

    public Medicine findMedicineById(Long id){
        return medicineRepo.findMedicineById(id);
    }
    public List<Medicine> findMedicinesByPersonTgId(long id){
        return medicineRepo.findMedicinesByPersonTgId(id);
    }
    @Transactional
    public void deleteMedicineById(Long medicineId) {
        medicineRepo.deleteById(medicineId);
    }

}
