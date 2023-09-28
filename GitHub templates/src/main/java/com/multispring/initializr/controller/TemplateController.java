package com.multispring.initializr.controller;

import com.multispring.initializr.utils.SpringBootProjectInitializer;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.StringWriter;

@RestController
public class TemplateController {

    private final Configuration freeMarkerConfiguration;

    public TemplateController(Configuration freeMarkerConfiguration) {
        this.freeMarkerConfiguration = freeMarkerConfiguration;
    }

    @GetMapping("/createApplication")
    public String createApplication() {
        return renderTemplate("inputForm");
    }

    @PostMapping("/processForm")
    public void processForm(
            @RequestParam("userInput") String projectName,
            @RequestParam("dropdownButton") String javaVersion,
            @RequestParam(value = "multiselectButton",required = false) List<String> addOns,
            @RequestParam(value = "addonOption", required = false) String database,
            HttpServletResponse response
    ) throws IOException, InterruptedException {
        SpringBootProjectInitializer.createSpringBootProject(projectName,javaVersion,
                addOns , database , response);
    }

    private String renderTemplate(String templateName) {
        return renderTemplate(templateName, new HashMap<>());
    }

    private String renderTemplate(String templateName, Map<String, Object> model) {
        try {
            Template template = freeMarkerConfiguration.getTemplate(templateName + ".ftl");
            StringWriter stringWriter = new StringWriter();
            template.process(model, stringWriter);
            return stringWriter.toString();
        } catch (IOException | TemplateException e) {
            e.printStackTrace();
            return "Error rendering template: " + templateName;
        }
    }
}
