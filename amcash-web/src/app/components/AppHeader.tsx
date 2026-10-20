import Image from "next/image";
import { ReactNode } from "react";

export default function AppHeader({
  children,
  className,
}: {
  children?: ReactNode;
  className?: string;
}) {
  return <header className={`dashboard-header${className ? ` ${className}` : ""}`}>
    <div className="header-brand" aria-label="AMCash">
      <Image src="/amcash-logo.png" alt="" width={1254} height={1254} priority />
      <span><b>AM</b>Cash</span>
    </div>
    {children}
  </header>;
}
