package mainpackage.medicinetakereminderbot.Services;

import mainpackage.medicinetakereminderbot.Models.Medicine;
import mainpackage.medicinetakereminderbot.Models.User;
import mainpackage.medicinetakereminderbot.Repositories.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepo userRepo;

    @Autowired
    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Transactional
    public void save(User user) {
        userRepo.save(user);
    }

    public Optional<User> findByTgId(Long id) {
        return userRepo.findByUserTgId(id);
    }



}
