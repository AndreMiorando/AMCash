import type { ApiUsageProfile, ApiWorkEarningSource } from "@/lib/api";

export type WorkPlatformKey = NonNullable<ApiWorkEarningSource["systemKey"]>;
export type WorkPlatform = { key: WorkPlatformKey; name: string; logo: string; className: string };

const platforms: Record<WorkPlatformKey, WorkPlatform> = {
  UBER: { key: "UBER", name: "Uber", logo: "/uber-logo.jpeg", className: "uber" },
  NINETY_NINE: { key: "NINETY_NINE", name: "99", logo: "/99-logo.png", className: "ninety-nine" },
  IFOOD: { key: "IFOOD", name: "iFood", logo: "/ifood-logo.png", className: "ifood" },
  NINETY_NINE_FOOD: { key: "NINETY_NINE_FOOD", name: "99Food", logo: "/99food-logo.png", className: "ninety-nine-food" },
};

const rideKeys: [WorkPlatformKey, WorkPlatformKey] = ["UBER", "NINETY_NINE"];
const deliveryKeys: [WorkPlatformKey, WorkPlatformKey] = ["IFOOD", "NINETY_NINE_FOOD"];
const normalize = (value: string) => value.trim().toLocaleLowerCase("pt-BR");

export function profileWorkPlatforms(profile: ApiUsageProfile | null | undefined): [WorkPlatform, WorkPlatform] {
  const [first, second] = profile === "DELIVERY" ? deliveryKeys : rideKeys;
  return [platforms[first], platforms[second]];
}

export function workPlatformByKey(key: WorkPlatformKey) {
  return platforms[key];
}

export function workPlatformBySource(source: string) {
  const normalized = normalize(source);
  return Object.values(platforms).find((platform) => normalize(platform.name) === normalized) ?? null;
}

export function isWorkPlatformSource(platform: WorkPlatform, source: string) {
  return normalize(platform.name) === normalize(source);
}
