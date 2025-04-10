package mainpackage.medicinetakereminderbot.Repositories;

import mainpackage.medicinetakereminderbot.Models.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRepo extends JpaRepository<Medicine, Long> {
    List<Medicine> findMedicinesByPersonTgId(Long id);
    Medicine findMedicineById(Long id);
}
