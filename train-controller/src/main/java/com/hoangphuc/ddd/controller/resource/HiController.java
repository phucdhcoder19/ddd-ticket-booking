package com.hoangphuc.ddd.controller.resource;

import com.hoangphuc.ddd.application.service.event.EventAppService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;

@RestController
@RequestMapping("/hello")
public class HiController {
    @Autowired
    private EventAppService eventAppService;

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/hi")
    @RateLimiter(name = "backendA", fallbackMethod = "fallbackHi")
    public String hello(){
//        return "hello";
        return eventAppService.sayHi("Hi");
    }

    private String fallbackHi(Throwable t) {
        return "fallback hello";
    }

    @GetMapping("/hi/v1")
    @RateLimiter(name = "backendB", fallbackMethod = "fallbackHi")
    public String sayhi(){
//        return "hello";
        return eventAppService.sayHi("Ho");
    }

    private static final SecureRandom secureRandom = new SecureRandom();
    @GetMapping("/circuit-breaker")
    @CircuitBreaker(name = "checkRandom", fallbackMethod = "fallbackCircuitBreaker")
    public String circuitBreaker() {
        int productId = secureRandom.nextInt(20) + 1; // Random product ID between 1 and 10
        String url = "https://fakestoreapi.com/products/" + productId;

        return restTemplate.getForObject(url, String.class);
    }

    private String fallbackCircuitBreaker(Throwable t) {
        return "fallback circuit breaker";
    }
}