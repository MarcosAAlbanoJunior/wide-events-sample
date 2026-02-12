package com.demo.wideevents.controller;

import com.demo.wideevents.domain.WideEventContext;
import com.demo.wideevents.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> checkout(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        var wideEvent = WideEventContext.current();

        var result = checkoutService.processCheckout(userId, wideEvent);

        var success = (boolean) result.get("success");
        return success
                ? ResponseEntity.ok(result)
                : ResponseEntity.unprocessableEntity().body(result);
    }
}
