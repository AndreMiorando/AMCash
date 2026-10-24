package br.com.amcash.vehicle.controller;

import br.com.amcash.config.*; import br.com.amcash.vehicle.dto.request.*; import br.com.amcash.vehicle.dto.response.*; import br.com.amcash.vehicle.service.VehicleService;
import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile; import java.util.*;

@RestController @RequestMapping("/api/v1/vehicles")
public class VehicleController {
    private final VehicleService service; public VehicleController(VehicleService service){this.service=service;}
    @GetMapping public List<VehicleResponse> list(@CurrentUser AuthenticatedUser user){return service.list(user.userId());}
    @GetMapping("/{id}") public VehicleResponse get(@CurrentUser AuthenticatedUser user,@PathVariable UUID id){return service.get(user.userId(),id);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public VehicleResponse create(@CurrentUser AuthenticatedUser user,@Valid @RequestBody VehicleRequest request){return service.create(user.userId(),request);}
    @PutMapping("/{id}") public VehicleResponse update(@CurrentUser AuthenticatedUser user,@PathVariable UUID id,@Valid @RequestBody VehicleRequest request){return service.update(user.userId(),id,request);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@CurrentUser AuthenticatedUser user,@PathVariable UUID id){service.delete(user.userId(),id);}
    @PostMapping("/{id}/odometer") @ResponseStatus(HttpStatus.CREATED) public OdometerReadingResponse odometer(@CurrentUser AuthenticatedUser user,@PathVariable UUID id,@Valid @RequestBody OdometerRequest request){return service.updateOdometer(user.userId(),id,request);}
    @PostMapping("/{id}/maintenances") @ResponseStatus(HttpStatus.CREATED) public MaintenanceResponse maintenance(@CurrentUser AuthenticatedUser user,@PathVariable UUID id,@Valid @RequestBody MaintenanceRequest request){return service.addMaintenance(user.userId(),id,request);}
    @PutMapping("/{id}/maintenances/{maintenanceId}") public MaintenanceResponse updateMaintenance(@CurrentUser AuthenticatedUser user,@PathVariable UUID id,@PathVariable UUID maintenanceId,@Valid @RequestBody MaintenanceRequest request){return service.updateMaintenance(user.userId(),id,maintenanceId,request);}
    @DeleteMapping("/{id}/maintenances/{maintenanceId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteMaintenance(@CurrentUser AuthenticatedUser user,@PathVariable UUID id,@PathVariable UUID maintenanceId){service.deleteMaintenance(user.userId(),id,maintenanceId);}
    @PutMapping(value="/{id}/photo",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.NO_CONTENT) public void photo(@CurrentUser AuthenticatedUser user,@PathVariable UUID id,@RequestPart("file") MultipartFile file){service.savePhoto(user.userId(),id,file);}
    @GetMapping("/{id}/photo") public ResponseEntity<byte[]> photo(@CurrentUser AuthenticatedUser user,@PathVariable UUID id){var photo=service.photo(user.userId(),id);return ResponseEntity.ok().contentType(MediaType.parseMediaType(photo.contentType())).cacheControl(CacheControl.noStore()).body(photo.content());}
    @DeleteMapping("/{id}/photo") @ResponseStatus(HttpStatus.NO_CONTENT) public void deletePhoto(@CurrentUser AuthenticatedUser user,@PathVariable UUID id){service.deletePhoto(user.userId(),id);}
}
