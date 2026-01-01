import { useState, useRef, useEffect } from "react";
import { ChevronDown, X } from "lucide-react";
import { cn } from "@/lib/utils";

interface ComboboxOption {
  value: string;
  label: string;
}

interface ComboboxProps {
  options: ComboboxOption[];
  value?: string;
  onChange: (value: string) => void;
  placeholder?: string;
  label?: string;
  allowCustomInput?: boolean;
  onCustomInput?: (input: string) => void;
}

export function Combobox({
  options,
  value,
  onChange,
  placeholder = "Select option...",
  label,
  allowCustomInput = false,
  onCustomInput,
}: ComboboxProps) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [inputValue, setInputValue] = useState("");
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Find selected option label
  const selectedLabel = options.find((opt) => opt.value === value)?.label || "";

  // Filter options based on search
  const filteredOptions = options.filter((opt) =>
    opt.label.toLowerCase().includes(search.toLowerCase()) ||
    opt.value.toLowerCase().includes(search.toLowerCase())
  );

  // Handle clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
        setSearch("");
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Focus input when opening
  useEffect(() => {
    if (open && inputRef.current) {
      inputRef.current.focus();
    }
  }, [open]);

  const handleSelect = (optionValue: string) => {
    onChange(optionValue);
    setOpen(false);
    setSearch("");
    setInputValue("");
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setSearch(val);
    setInputValue(val);
    if (allowCustomInput && onCustomInput) {
      onCustomInput(val);
    }
  };

  const handleClear = (e: React.MouseEvent) => {
    e.stopPropagation();
    onChange("");
    setSearch("");
    setInputValue("");
  };

  return (
    <div ref={containerRef} className="relative w-full">
      {label && <label className="text-sm font-medium block mb-2">{label}</label>}

      <button
        onClick={() => setOpen(!open)}
        type="button"
        className="w-full relative px-3 py-2 rounded-md border bg-background text-left outline-none focus:ring-2 focus:ring-ring flex items-center justify-between"
      >
        <div className="flex-1 flex items-center gap-2">
          {value && !open ? (
            <span className="text-foreground">{selectedLabel || inputValue}</span>
          ) : (
            <span className="text-muted-foreground">{placeholder}</span>
          )}
        </div>
        {value && open && (
          <X
            className="h-4 w-4 text-muted-foreground hover:text-foreground cursor-pointer"
            onClick={handleClear}
          />
        )}
        {!value && (
          <ChevronDown
            className={cn(
              "h-4 w-4 text-muted-foreground transition-transform",
              open && "rotate-180"
            )}
          />
        )}
      </button>

      {open && (
        <div className="absolute top-full left-0 right-0 mt-1 bg-background border rounded-md shadow-lg z-50">
          <input
            ref={inputRef}
            type="text"
            placeholder={`Tìm kiếm...`}
            value={search}
            onChange={handleInputChange}
            className="w-full px-3 py-2 border-b outline-none focus:ring-2 focus:ring-ring bg-background"
          />

          <div className="max-h-60 overflow-y-auto">
            {filteredOptions.length > 0 ? (
              filteredOptions.map((option) => (
                <button
                  key={option.value}
                  onClick={() => handleSelect(option.value)}
                  type="button"
                  className={cn(
                    "w-full text-left px-3 py-2 hover:bg-muted text-sm",
                    value === option.value && "bg-primary/10 text-primary font-medium"
                  )}
                >
                  {option.label}
                </button>
              ))
            ) : (
              <div className="px-3 py-2 text-sm text-muted-foreground text-center">
                Không tìm thấy
              </div>
            )}
          </div>

          {allowCustomInput && search && !filteredOptions.some(opt => opt.value === search) && (
            <button
              onClick={() => {
                handleSelect(search);
              }}
              type="button"
              className="w-full text-left px-3 py-2 hover:bg-muted text-sm border-t font-medium text-primary"
            >
              Sử dụng "{search}"
            </button>
          )}
        </div>
      )}
    </div>
  );
}
