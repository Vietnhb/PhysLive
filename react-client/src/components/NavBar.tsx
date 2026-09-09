import { Link } from "react-router-dom";
import "../styles/NavBar.css"
import { getToken, clearToken } from "../utils/token";
type NavBarProps = { onChange: (value: "home" | "support" | "care") => void };
function NavBar({ onChange }: NavBarProps) {
    const token = getToken();
    const handleLogout = () => {
        clearToken();
        window.location.reload();
    }

    return (
        <div className="navbar">
            <div className="logo"><Link to={"/"} onClick={() => onChange("home")}>MyWeb</Link></div>
            <div className="menu">
                <button onClick={() => onChange("home")}>Home</button>
                <button onClick={() => onChange("support")}>Hỗ Trợ</button>
                <button onClick={() => onChange("care")}>Chăm Sóc</button>
                {!token && (<Link to="/Login">Login</Link>)}
                {token && (<button onClick={handleLogout} >Logout</button>)}
            </div>
        </div>
    )
}
export default NavBar;