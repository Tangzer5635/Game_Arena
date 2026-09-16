interface Props {
    value: string;
    onChange: (v: string) => void;
    placeholder?: string;
}
export default function SearchBar({ value, onChange, placeholder = "Rechercher..." }: Props) {
    return (
        <div className="search-bar">
            <span className="search-icon">⌕</span>
            <input type="text" value={value} onChange={e => onChange(e.target.value)} placeholder={placeholder} />
            {value && <button className="search-clear btn-ghost" onClick={() => onChange("")}>✕</button>}
        </div>
    );
}