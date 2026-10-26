"use client";

import { useState } from "react";
import { ApiWorkCostCategory } from "@/lib/api";
import { CostCategoryGrid, CostCategoryIcon } from "@/app/components/CostCategoryGrid";
import Button from "@/app/components/ui/Button";
import { InputField } from "@/app/components/ui/Input";
import { completeCurrencyInput, maskCurrencyInput, parseCurrencyInput } from "@/lib/currency";

export type CostDialogValue = {
  category: ApiWorkCostCategory;
  amount: string;
  description: string;
};

export default function CostDialog({ onClose, onAdd }: {
  onClose: () => void;
  onAdd: (value: CostDialogValue) => void;
}) {
  const [category, setCategory] = useState<ApiWorkCostCategory>("FUEL");
  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState("");

  function save() {
    if ((parseCurrencyInput(amount) || 0) <= 0) {
      setError("Informe o valor do custo.");
      return;
    }
    onAdd({ category, amount, description: description.trim() });
  }

  return <div className="sheet-backdrop confirm-backdrop work-entry-backdrop" role="presentation" onMouseDown={onClose}>
    <section className="work-entry-dialog" role="dialog" aria-modal="true" aria-labelledby="cost-dialog-title" onMouseDown={(event) => event.stopPropagation()}>
      <header><span className="work-entry-dialog-icon cost"><CostCategoryIcon category={category} /></span><h2 id="cost-dialog-title">Adicionar custo</h2><p>Informe a categoria e os dados do custo.</p></header>
      <div className="work-entry-dialog-fields">
        <div className="work-entry-category"><span>Categoria</span><CostCategoryGrid value={category} ariaLabel="Categoria do custo" onChange={(value) => { setCategory(value); setError(""); }} /></div>
        <InputField label="Valor" prefix="R$" inputMode="numeric" placeholder="0,00" value={amount} onChange={(event) => { setAmount(maskCurrencyInput(event.target.value)); setError(""); }} onBlur={() => setAmount((value) => completeCurrencyInput(value))} />
        <InputField label="Descrição (opcional)" maxLength={60} placeholder="Ex.: Abastecimento no posto" value={description} onChange={(event) => setDescription(event.target.value)} />
        {error && <p role="alert">{error}</p>}
      </div>
      <footer><Button type="button" fullWidth className="work-entry-save" onClick={save}>Salvar custo</Button><Button type="button" variant="danger" fullWidth className="work-entry-cancel" onClick={onClose}>Cancelar</Button></footer>
    </section>
  </div>;
}
