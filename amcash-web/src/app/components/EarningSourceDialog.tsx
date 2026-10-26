"use client";

import { useState } from "react";
import Button from "@/app/components/ui/Button";
import { InputField } from "@/app/components/ui/Input";
import SegmentedControl from "@/app/components/ui/SegmentedControl";
import { maskCurrencyInput, parseCurrencyInput } from "@/lib/currency";

export type EarningSourceDialogValue = {
  source: string;
  amount: string;
  persistForFuture: boolean;
};

type Availability = "TODAY" | "FUTURE";
const availabilityOptions: { value: Availability; label: string }[] = [
  { value: "TODAY", label: "Somente hoje" },
  { value: "FUTURE", label: "Próximos registros" },
];

export default function EarningSourceDialog({ existingSources, onClose, onAdd }: {
  existingSources: string[];
  onClose: () => void;
  onAdd: (value: EarningSourceDialogValue) => void;
}) {
  const [source, setSource] = useState("");
  const [amount, setAmount] = useState("");
  const [availability, setAvailability] = useState<Availability>("TODAY");
  const [error, setError] = useState("");

  function save() {
    const name = source.trim();
    const normalized = name.toLocaleLowerCase("pt-BR");
    if (!name) { setError("Informe o nome do ganho."); return; }
    if (existingSources.some((item) => item.trim().toLocaleLowerCase("pt-BR") === normalized)) {
      setError("Esse ganho já existe.");
      return;
    }
    if (availability === "TODAY" && (parseCurrencyInput(amount) || 0) <= 0) {
      setError("Informe o valor recebido hoje.");
      return;
    }
    onAdd({ source: name, amount, persistForFuture: availability === "FUTURE" });
  }

  return <div className="sheet-backdrop confirm-backdrop work-entry-backdrop" role="presentation" onMouseDown={onClose}>
    <section className="work-entry-dialog" role="dialog" aria-modal="true" aria-labelledby="earning-source-dialog-title" onMouseDown={(event) => event.stopPropagation()}>
      <header><span className="work-entry-dialog-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><rect x="3" y="6" width="18" height="13" rx="2" /><path d="M16 10h5v5h-5a2.5 2.5 0 0 1 0-5ZM7 6V4h10v2" /></svg></span><h2 id="earning-source-dialog-title">Adicionar ganho</h2><p>Informe a origem e escolha onde ele será exibido.</p></header>
      <div className="work-entry-dialog-fields">
        <InputField label="Nome do ganho" maxLength={30} placeholder="Ex.: Particular" value={source} onChange={(event) => { setSource(event.target.value); setError(""); }} />
        <InputField label={availability === "TODAY" ? "Valor de hoje" : "Valor de hoje (opcional)"} prefix="R$" inputMode="numeric" placeholder="0,00" value={amount} onChange={(event) => { setAmount(maskCurrencyInput(event.target.value)); setError(""); }} />
        <div className="earning-source-availability"><span>Disponibilidade</span><SegmentedControl ariaLabel="Disponibilidade do ganho" value={availability} options={availabilityOptions} onChange={(value) => { setAvailability(value); setError(""); }} /></div>
        {error && <p role="alert">{error}</p>}
      </div>
      <footer><Button type="button" fullWidth className="work-entry-save" onClick={save}>Salvar ganho</Button><Button type="button" variant="danger" fullWidth className="work-entry-cancel" onClick={onClose}>Cancelar</Button></footer>
    </section>
  </div>;
}
