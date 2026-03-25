package org.trainbeans.clearancecard.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.trainbeans.clearancecard.exception.ClearanceCardNotFoundException;
import org.trainbeans.clearancecard.model.ClearanceCard;
import org.trainbeans.clearancecard.model.ClearanceCardRequest;
import org.trainbeans.clearancecard.service.ClearanceCardService;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/api/clearance-cards")
public class ClearanceCardController {

    private final ClearanceCardService service;

    @Value("${app.railroad.name}")
    private String railroadName;

    public ClearanceCardController(ClearanceCardService service) {
        this.service = service;
    }

    /** Create a new clearance card. Returns 201 Created with Location header. */
    @PostMapping(consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<ClearanceCard> create(@RequestBody ClearanceCardRequest request) {
        ClearanceCard created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * List all clearance cards, optionally filtered by issued date.
     * Example: GET /api/clearance-cards?date=2026-03-24
     */
    @GetMapping(produces = "application/json")
    @ResponseBody
    public List<ClearanceCard> list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.findAll(date);
    }

    /** Retrieve a single clearance card as JSON. */
    @GetMapping(value = "/{id}", produces = "application/json")
    @ResponseBody
    public ClearanceCard getById(@PathVariable Long id) {
        return service.findById(id);
    }

    /** Render the Form 427-A HTML page, suitable for browser printing. */
    @GetMapping("/{id}/print")
    public String print(@PathVariable Long id, Model model) {
        ClearanceCard card = service.findById(id);
        model.addAttribute("card", card);
        model.addAttribute("railroadName", railroadName);
        return "clearance-card-print";
    }

    /** Render a compact receipt layout sized for a 58 mm thermal printer. */
    @GetMapping("/{id}/receipt")
    public String receipt(@PathVariable Long id, Model model) {
        ClearanceCard card = service.findById(id);
        model.addAttribute("card", card);
        model.addAttribute("railroadName", railroadName);
        return "clearance-card-receipt";
    }

    @ExceptionHandler(ClearanceCardNotFoundException.class)
    @ResponseBody
    public ResponseEntity<Void> handleNotFound(ClearanceCardNotFoundException ex) {
        return ResponseEntity.notFound().build();
    }
}

