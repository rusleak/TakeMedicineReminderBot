package mainpackage.medicinetakereminderbot.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mainpackage.medicinetakereminderbot.Enums.MedicineState;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class Medicine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    @Column(name = "medicine_name")
    private String medicineName;
    @Column(name = "per_day")
    private Integer perDay;
    @Column(name = "how_often_per_day")
    private Integer howOftenPerDay;
    @Column(name = "person_tg_id")
    private Long personTgId;
    @Column(name = "person_username")
    private String personUsername;
    @Column(name = "state")
    private MedicineState state;
    @Column(name = "counter", columnDefinition = "BIGINT DEFAULT 0")
    private Long counter;
    // Связь с User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @PrePersist
    public void setDefaultValues() {
        if (this.counter == null) {
            this.counter = 0L;
        }
    }

}
