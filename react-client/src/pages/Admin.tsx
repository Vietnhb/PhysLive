import { useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import type { User } from "../types/user";
import { getUsers } from "../api/userApi";
import { getToken } from "../utils/token";
import { getMe } from "../api/userApi";
function Admin() {
    const navigate = useNavigate();
    const [error, setError] = useState();
    const [users, setUsers] = useState<User[]>([]);
    const token = getToken();
    useEffect(() => {
        if (!token) navigate("/");
        getMe().then((data) => { if (data.role != "ADMIN") { navigate("/") } });
    }, [])
    useEffect(() => {
        getUsers().then((data) => { setUsers(data); })
            .catch((err: any) => {
                setError(err.response.data.message)
            })
    }, [])

    return (
        <>
            <h1>Trang Admin</h1>
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Email</th>
                        <th>FullName</th>
                        <th>Role</th>
                    </tr>
                </thead>
                <tbody>

                    {users.map((user) => (
                        <tr key={user.id} >
                            <td>{user.id}</td>
                            <td><input type="email" value={user.email} placeholder="Email" ></input></td>
                            <td><input type="text" value={user.fullName} placeholder="FullName" ></input></td>
                            <td><input type="text" value={user.role} placeholder="Role" ></input></td>
                            <td><button>Delete</button></td>
                            <td><button>Update</button></td>
                        </tr>
                    ))}
                </tbody>
            </table >

            {error && <span style={{ color: "red" }} >{error}</span>
            }
            <button onClick={() => { navigate("/") }} >Ve Trang Chu</button>
        </>
    )
}
export default Admin;