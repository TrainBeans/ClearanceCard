package org.trainbeans.clearancecard.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.trainbeans.clearancecard.model.ClearanceCardRequest;
import org.trainbeans.clearancecard.service.ClearanceCardService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Handles all browser-facing UI pages for clearance card management.
 * Paths are under /ui/clearance-cards to keep them separate from the JSON API.
 */
@Controller
@RequestMapping("/ui/clearance-cards")
public class UiController {

    private final ClearanceCardService service;

    @Value("${app.railroad.name}")
    private String railroadName;

    public UiController(ClearanceCardService service) {
        this.service = service;
    }

    /** List page — optionally filtered by date. */
    @GetMapping
    public String list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        model.addAttribute("cards", service.findAll(date));
        model.addAttribute("filterDate", date);
        model.addAttribute("railroadName", railroadName);
        return "ui/card-list";
    }

    /** Show the blank entry form, pre-filled with today's date and current time. */
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("nowTime", LocalTime.now().truncatedTo(ChronoUnit.MINUTES));
        model.addAttribute("railroadName", railroadName);
        return "ui/card-form";
    }

    /** Accept the posted form, persist the card, then redirect to the list. */
    @PostMapping
    public String create(
            @RequestParam String station,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime issuedTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issuedDate,
            @RequestParam String trainNumber,
            @RequestParam int orderCount,
            @RequestParam(required = false, defaultValue = "") String signalStopFor,
            @RequestParam String operatorName,
            @RequestParam(required = false) List<String> orderNumbers) {

        List<String> filtered = (orderNumbers == null) ? List.of()
                : orderNumbers.stream().filter(s -> s != null && !s.isBlank()).toList();

        service.create(new ClearanceCardRequest(
                station, issuedTime, issuedDate,
                trainNumber, orderCount, signalStopFor,
                operatorName, filtered));

        return "redirect:/ui/clearance-cards";
    }
}

