package com.example.cardealership;

import com.example.cardealership.entity.Car;
import com.example.cardealership.repository.CarRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final CarRepository carRepository;

    public DataLoader(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    @Override
    public void run(String... args) {
        if (carRepository.count() == 0) {
            carRepository.save(new Car("Toyota", "Camry", 2023, "Silver", 28000));
            carRepository.save(new Car("Honda", "Civic", 2022, "Blue", 24000));
            carRepository.save(new Car("Ford", "Mustang", 2024, "Red", 45000));
            carRepository.save(new Car("Tesla", "Model 3", 2024, "White", 42000));
            carRepository.save(new Car("BMW", "330i", 2023, "Black", 46000));
            System.out.println("\n=== Seeded " + carRepository.count() + " cars ===");
        } else {
            System.out.println("\n=== Database already has " + carRepository.count() + " cars — skipping seed ===");
        }
    }
}
