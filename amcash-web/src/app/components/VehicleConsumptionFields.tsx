import { InputField } from "@/app/components/ui/Input";
import type { VehicleDraft } from "@/app/components/VehicleScreen";

type ConsumptionKey = "gasolineConsumption" | "ethanolConsumption" | "dieselConsumption" | "electricConsumption";

export default function VehicleConsumptionFields({draft,onChange}:{draft:VehicleDraft;onChange:(key:ConsumptionKey,value:string)=>void}){
  const field=(label:string,key:ConsumptionKey,suffix="km/L",required=false)=><InputField label={label} suffix={suffix} inputMode="decimal" required={required} value={draft[key]} onChange={(event)=>onChange(key,event.target.value)}/>;
  if(draft.fuelType==="FLEX")return <>{field("Consumo gasolina (opcional)","gasolineConsumption")}{field("Consumo etanol (opcional)","ethanolConsumption")}</>;
  if(draft.fuelType==="ETHANOL")return field("Consumo médio (opcional)","ethanolConsumption");
  if(draft.fuelType==="ELECTRIC")return field("Consumo médio (opcional)","electricConsumption","kWh/100 km");
  if(draft.fuelType==="DIESEL")return field("Consumo médio","dieselConsumption","km/L",true);
  if(draft.fuelType==="HYBRID")return <>{field("Consumo gasolina (opcional)","gasolineConsumption")}{field("Consumo etanol (opcional)","ethanolConsumption")}{field("Consumo diesel (opcional)","dieselConsumption")}{field("Consumo elétrico (opcional)","electricConsumption","kWh/100 km")}</>;
  return field("Consumo médio (opcional)","gasolineConsumption");
}
