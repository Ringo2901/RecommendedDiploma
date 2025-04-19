package by.bsuir.aleksandrov.recommendeddiploma.controller;

import by.bsuir.aleksandrov.recommendeddiploma.model.Schema;
import by.bsuir.aleksandrov.recommendeddiploma.service.SchemaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/schema")
public class SchemaController {
    private final SchemaService schemaService;

    public SchemaController(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @GetMapping
    public String viewSchemas(Model model) {
        List<Schema> schemas = schemaService.getAllSchemas();
        model.addAttribute("schemas", schemas);
        return "schema-list";
    }

    @GetMapping("/add")
    public String addSchemaForm(Model model) {
        model.addAttribute("schema", new Schema());
        return "add-schema";
    }

    @PostMapping("/add")
    public String addSchema(@ModelAttribute Schema schema) {
        schemaService.saveSchema(schema);
        return "redirect:/admin/schema";
    }

    @GetMapping("/delete/{id}")
    public String deleteSchema(@PathVariable String id) {
        schemaService.deleteSchema(id);
        return "redirect:/admin/schema";
    }
}
