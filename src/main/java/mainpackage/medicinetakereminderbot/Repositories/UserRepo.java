package mainpackage.medicinetakereminderbot.Repositories;

import mainpackage.medicinetakereminderbot.Models.Medicine;
import mainpackage.medicinetakereminderbot.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, Long> {

    Optional<User> findByUserTgId(Long Long);


}
