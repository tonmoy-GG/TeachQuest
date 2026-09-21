package com.teachquest.controller;

import com.teachquest.model.User;
import com.teachquest.service.AdminAccessService;
import com.teachquest.service.AdminUserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "*")
public class AdminUserController {
    private final AdminAccessService accessService;
    private final AdminUserService userService;

    public AdminUserController(AdminAccessService accessService, AdminUserService userService) {
        this.accessService = accessService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestHeader(value = "X-User-Id", required = false) Long adminId,
                                  @RequestParam(required = false) String role,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String search,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        try {
            accessService.requireAdmin(adminId);
            return ResponseEntity.ok(userService.list(role, status, search,
                    PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt"))));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.status(403).body(error.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@RequestHeader(value = "X-User-Id", required = false) Long adminId, @PathVariable Long id) {
        try {
            accessService.requireAdmin(adminId);
            return ResponseEntity.ok(userService.detail(id));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.status(403).body(error.getMessage());
        } catch (NoSuchElementException error) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> status(@RequestHeader(value = "X-User-Id", required = false) Long adminId,
                                    @PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            accessService.requireAdmin(adminId);
            User user = userService.updateStatus(id, body.get("status"));
            return ResponseEntity.ok(Map.of("id", user.getId(), "status", user.getStatus()));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.status(403).body(error.getMessage());
        } catch (NoSuchElementException error) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<?> role(@RequestHeader(value = "X-User-Id", required = false) Long adminId,
                                  @PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            accessService.requireAdmin(adminId);
            User user = userService.updateRole(id, body.get("role"));
            return ResponseEntity.ok(Map.of("id", user.getId(), "role", user.getUserType()));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.status(403).body(error.getMessage());
        } catch (NoSuchElementException error) {
            return ResponseEntity.notFound().build();
        }
    }
}
