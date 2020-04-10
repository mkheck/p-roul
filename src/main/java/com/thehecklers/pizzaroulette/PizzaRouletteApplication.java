package com.thehecklers.pizzaroulette;

import lombok.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@SpringBootApplication
public class PizzaRouletteApplication {
	@Bean
	CommandLineRunner demoData(PizzaRepo repo) {
		return args -> {
			repo.save(new Pizza(1L, "Pacific Veggie"));
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(PizzaRouletteApplication.class, args);
	}

}

@EnableBinding(Source.class)
@Controller
@RequiredArgsConstructor
class PizzaPlanet {
	@NonNull
	private final PizzaRepo repo;
	private final Source source;

/*
	@Bean
	Supplier<Pizza> sendOrderInfo() {
		return () -> {
			System.out.println(this.pizza);
			return repo.save(this.pizza);
		};
	}
*/

	@GetMapping("/")
	public String order(Model model) {
		model.addAttribute("pizzas", repo.findAll());
		model.addAttribute("pizza", new Pizza());
		return "orderpizza";
	}

	@PostMapping("/savepizza")
	public String savePizza(@ModelAttribute Pizza pizza, Model model) {
		model.addAttribute("pizzas", repo.findAll());

		Pizza newPizza = repo.save(pizza);
		System.out.println(newPizza);
		source.output().send(MessageBuilder.withPayload(newPizza).build());
		model.addAttribute("pizza", newPizza);

		return "redirect:/";
	}
}

@RestController
@RequestMapping("/pizzas")
@AllArgsConstructor
class PizzaAPI {
	private final PizzaRepo repo;

	@GetMapping
	Iterable<Pizza> getAllPizzas() {
		return repo.findAll();
	}

	@GetMapping("/{id}")
	Optional<Pizza> getPizzaById(@PathVariable Long id) {
		return repo.findById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	Pizza savePizza(@RequestBody Pizza pizza) {
		return repo.save(pizza);
	}
}

interface PizzaRepo extends CrudRepository<Pizza, Long> {}

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
class Pizza {
	@Id
	private Long id;
	@NonNull
	private String description;
}