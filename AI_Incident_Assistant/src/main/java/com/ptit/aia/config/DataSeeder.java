package com.ptit.aia.config;

import com.ptit.aia.domain.Engineer;
import com.ptit.aia.repository.EngineerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {
    private final EngineerRepository engineerRepository;

    public DataSeeder(EngineerRepository engineerRepository) {
        this.engineerRepository = engineerRepository;
    }

    @Override
    public void run(String... args) {
        if (engineerRepository.count() > 0) {
            return;
        }
        seed("le_thi_b", "Le Thi B", "Auth/Login, Backend", "Login,Backend/API", 1);
        seed("pham_van_c", "Pham Van C", "Backend, Payment", "Payment,Backend/API", 2);
        seed("nguyen_thi_d", "Nguyen Thi D", "Fullstack, UI", "Frontend/UI,Login", 0);
    }

    private void seed(String username, String displayName, String skills, String access, int workload) {
        Engineer engineer = new Engineer();
        engineer.setUsername(username);
        engineer.setDisplayName(displayName);
        engineer.setSkills(skills);
        engineer.setAccessComponents(access);
        engineer.setActiveIncidentCount(workload);
        engineer.setOnline(true);
        engineerRepository.save(engineer);
    }
}
