package org.kolar.kolarcodercopilot.config;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestSimpleAOPController {
    @PostMapping("/test")
    public String testSimpleAOPController(@RequestParam String name,@RequestParam String content){
        return "name : " + name + ", content : " + content;
    }

}
