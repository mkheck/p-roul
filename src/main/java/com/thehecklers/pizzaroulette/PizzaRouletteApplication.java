package com.thehecklers.pizzaroulette;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@SpringBootApplication
public class PizzaRouletteApplication {
    @Bean
    CommandLineRunner demoData(PizzaRepo repo) {
        return args -> {
            System.out.println("<><><> Saving initial pizza <><><>");
            repo.deleteAll().then(
                    repo.save(new Pizza(1L, "Pacific Veggie")))
                    .subscribe(System.out::println);

/*            repo.deleteAll();
            repo.save(new Pizza(1L, "Pacific Veggie"));
            repo.findAll().forEach(System.out::println);*/
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(PizzaRouletteApplication.class, args);
    }

}

@EnableBinding(Source.class)
@Controller
class PizzaPlanet {
    private final PizzaRepo repo;
    private final Source source;

    PizzaPlanet(PizzaRepo repo, Source source) {
        this.repo = repo;
        this.source = source;
    }

    @GetMapping("/")
    public String order(Model model) {
        model.addAttribute("pizzas", repo.findAll());
        model.addAttribute("pizza", new Pizza(""));
        return "orderpizza";
    }

    @PostMapping("/savepizza")
    public String savePizza(@ModelAttribute Pizza pizza, Model model) {
        source.output().send(MessageBuilder.withPayload(pizza).build());

        model.addAttribute("pizzas", repo.findAll());
        model.addAttribute("pizza", repo.save(pizza));

        return "redirect:/";
    }
}

@EnableBinding(Source.class)
@RestController
@RequestMapping("/pizzas")
class PizzaAPI {
    private final PizzaRepo repo;
    private final Source source;

    PizzaAPI(PizzaRepo repo, Source source) {
        this.repo = repo;
        this.source = source;
    }

    @GetMapping
    Flux<Pizza> getAllPizzas() {
    //Iterable<Pizza> getAllPizzas() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    Mono<Pizza> getPizzaById(@PathVariable Long id) {
    //Pizza getPizzaById(@PathVariable Long id) {
        return repo.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Mono<Pizza> savePizza(@RequestBody Pizza pizza) {
    //Pizza savePizza(@RequestBody Pizza pizza) {
        source.output().send(MessageBuilder.withPayload(pizza).build());
        return repo.save(pizza);
    }
}

interface PizzaRepo extends ReactiveCrudRepository<Pizza, Long> {
}

@Table
class Pizza {
    @PrimaryKey
    private Long id;
    private String description;

    public Pizza() {
        this("Plain pizza, no sauce");
    }

    public Pizza(String description) {
        this(0L, description);
    }

    public Pizza(Long id, String description) {
        this.id = id;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pizza pizza = (Pizza) o;
        return id.equals(pizza.id) &&
                description.equals(pizza.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, description);
    }

    @Override
    public String toString() {
        return "Pizza{" +
                "id=" + id +
                ", description='" + description + '\'' +
                '}';
    }
}
