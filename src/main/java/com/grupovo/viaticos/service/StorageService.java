package com.grupovo.viaticos.service;

import com.grupovo.viaticos.exception.InvalidEntryException;
import com.grupovo.viaticos.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Map;

@Service
public class StorageService {

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> EXTENSIONES_PERMITIDAS =
            Set.of("jpg", "jpeg", "png", "pdf", "gif", "webp");
    private static final Map<String, Set<String>> TIPOS_PERMITIDOS = Map.of(
            "pdf", Set.of("application/pdf"),
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"),
            "png", Set.of("image/png"),
            "gif", Set.of("image/gif"),
            "webp", Set.of("image/webp")
    );

    private final Path rootLocation;

    public StorageService(@Value("${app.storage.path:uploads}") String storagePath) {
        this.rootLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new InvalidEntryException("No se pudo inicializar el almacenamiento de archivos");
        }
    }

    public String guardar(MultipartFile archivo, String carpeta) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }
        if (archivo.getSize() > MAX_SIZE_BYTES) {
            throw new InvalidEntryException("El archivo excede el tamaño máximo permitido (10 MB)");
        }
        String nombreOriginal = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename();
        String extension = extension(nombreOriginal);
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new InvalidEntryException("Tipo de archivo no permitido: " + extension);
        }
        String contentType = archivo.getContentType() == null ? "" : archivo.getContentType().toLowerCase();
        if (!TIPOS_PERMITIDOS.getOrDefault(extension, Set.of()).contains(contentType)) {
            throw new InvalidEntryException("El contenido del archivo no coincide con su extensión");
        }

        Path carpetaPath = rootLocation.resolve(carpeta).normalize();
        if (!carpetaPath.startsWith(rootLocation)) {
            throw new InvalidEntryException("Carpeta de almacenamiento no válida");
        }
        try {
            Files.createDirectories(carpetaPath);
            String nombre = UUID.randomUUID() + "." + extension;
            Path destino = carpetaPath.resolve(nombre);
            try (InputStream in = archivo.getInputStream()) {
                Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            return carpeta + "/" + nombre;
        } catch (IOException e) {
            throw new InvalidEntryException("Error al guardar el archivo");
        }
    }

    public Resource cargar(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            throw new NotFoundException("Archivo no encontrado");
        }
        try {
            Path archivo = rootLocation.resolve(rutaRelativa).normalize();
            if (!archivo.startsWith(rootLocation) || !Files.exists(archivo)) {
                throw new NotFoundException("Archivo no encontrado");
            }
            Resource resource = new UrlResource(archivo.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new NotFoundException("Archivo no encontrado");
        } catch (MalformedURLException e) {
            throw new NotFoundException("Archivo no encontrado");
        }
    }

    public void eliminar(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            return;
        }
        Path archivo = rootLocation.resolve(rutaRelativa).normalize();
        if (!archivo.startsWith(rootLocation)) {
            throw new InvalidEntryException("Ruta de archivo no válida");
        }
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            throw new InvalidEntryException("No se pudo eliminar el archivo anterior");
        }
    }

    private String extension(String nombre) {
        int idx = nombre.lastIndexOf('.');
        if (idx < 0) {
            return "";
        }
        return nombre.substring(idx + 1).toLowerCase();
    }
}
