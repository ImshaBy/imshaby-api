package by.imsha.rest;

import by.imsha.domain.Cors;
import by.imsha.domain.dto.CorsInfo;
import by.imsha.domain.dto.UpdateEntityInfo;
import by.imsha.domain.dto.mapper.CorsMapper;
import by.imsha.exception.ResourceNotFoundException;
import by.imsha.service.CorsConfigService;
import by.imsha.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/cors")
@Slf4j
@Validated
public class CorsController {

    @Autowired
    private CorsConfigService corsConfigService;

    @Autowired
    private CorsMapper corsMapper;

    @PostMapping
    public ResponseEntity<Cors> createCors(@RequestBody CorsInfo corsInfo) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        this.corsConfigService.create(
                                Cors.builder()
                                        .origin(corsInfo.getOrigin())
                                        .build()
                        )
                );
    }

    @GetMapping
    public ResponseEntity<Page<Cors>> getAllCors(
            final @RequestParam(value = "page", defaultValue = Constants.DEFAULT_PAGE_NUM) int page,
            final @RequestParam(value = "size", defaultValue = Constants.DEFAULT_PAGE_SIZE) int size) {

        return ResponseEntity.ok(
                this.corsConfigService.getAll(page, size)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cors> getCors(final @PathVariable("id") String id) {
        return ResponseEntity.ok(
                this.corsConfigService.get(id)
                        .orElseThrow(ResourceNotFoundException::new));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UpdateEntityInfo> updateCors(final @PathVariable("id") String id, final @RequestBody CorsInfo corsInfo) {
        final Cors cors = this.corsConfigService.get(id)
                .orElseThrow(ResourceNotFoundException::new);

        corsMapper.updateCorsFromDTO(corsInfo, cors);

        final Cors updateCors = this.corsConfigService.update(cors);

        return ResponseEntity.ok(
                new UpdateEntityInfo(updateCors.getId(), UpdateEntityInfo.STATUS.UPDATED)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<UpdateEntityInfo> deleteCors(final @PathVariable("id") String id) {
        if (!this.corsConfigService.get(id).isPresent()) {
            throw new ResourceNotFoundException();
        }

        this.corsConfigService.remove(id);

        return ResponseEntity.ok(
                new UpdateEntityInfo(id, UpdateEntityInfo.STATUS.DELETED)
        );
    }
}
