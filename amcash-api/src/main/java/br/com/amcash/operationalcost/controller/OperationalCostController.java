package br.com.amcash.operationalcost.controller;
import br.com.amcash.config.*; import br.com.amcash.operationalcost.dto.request.OperationalCostRequest; import br.com.amcash.operationalcost.dto.response.OperationalCostResponse; import br.com.amcash.operationalcost.service.OperationalCostService; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/v1/operational-costs")
public class OperationalCostController {
    private final OperationalCostService service; public OperationalCostController(OperationalCostService service){this.service=service;}
    @GetMapping public List<OperationalCostResponse> list(@CurrentUser AuthenticatedUser user){return service.list(user.userId());}
    @GetMapping("/vehicles/{vehicleId}") public OperationalCostResponse get(@CurrentUser AuthenticatedUser user,@PathVariable UUID vehicleId){return service.get(user.userId(),vehicleId);}
    @PostMapping("/vehicles/{vehicleId}") @ResponseStatus(HttpStatus.CREATED) public OperationalCostResponse create(@CurrentUser AuthenticatedUser user,@PathVariable UUID vehicleId,@Valid @RequestBody OperationalCostRequest request){return service.create(user.userId(),vehicleId,request);}
    @PutMapping("/vehicles/{vehicleId}") public OperationalCostResponse update(@CurrentUser AuthenticatedUser user,@PathVariable UUID vehicleId,@Valid @RequestBody OperationalCostRequest request){return service.update(user.userId(),vehicleId,request);}
    @DeleteMapping("/vehicles/{vehicleId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@CurrentUser AuthenticatedUser user,@PathVariable UUID vehicleId){service.delete(user.userId(),vehicleId);}
}
