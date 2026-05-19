package com.raiz.bakcend.dto;

import java.util.UUID;

public class UsuarioResponseDTO {
    private UUID id;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String rol;
    private Boolean activo;
    private Boolean membresiaActiva;

    public UsuarioResponseDTO() {}

    public UsuarioResponseDTO(UUID id, String nombre, String apellido, String email, String telefono, String rol, Boolean activo, Boolean membresiaActiva) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.telefono = telefono;
        this.rol = rol;
        this.activo = activo;
        this.membresiaActiva = membresiaActiva;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public Boolean getMembresiaActiva() { return membresiaActiva; }
    public void setMembresiaActiva(Boolean membresiaActiva) { this.membresiaActiva = membresiaActiva; }
}
