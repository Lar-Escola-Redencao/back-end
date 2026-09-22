package br.org.larescolaredencao.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public class PosEventoDTO {

    private String comentarioPosEvento;
    private List<MultipartFile> imagens;
    private List<MultipartFile> videos;
    private List<String> imagensUrls;
    private List<String> videosUrls;

    public String getComentarioPosEvento() {
        return comentarioPosEvento;
    }

    public void setComentarioPosEvento(String comentarioPosEvento) {
        this.comentarioPosEvento = comentarioPosEvento;
    }

    public List<MultipartFile> getImagens() {
        return imagens;
    }

    public void setImagens(List<MultipartFile> imagens) {
        this.imagens = imagens;
    }

    public List<MultipartFile> getVideos() {
        return videos;
    }

    public void setVideos(List<MultipartFile> videos) {
        this.videos = videos;
    }

    public List<String> getImagensUrls() {
        return imagensUrls;
    }

    public void setImagensUrls(List<String> imagensUrls) {
        this.imagensUrls = imagensUrls;
    }

    public List<String> getVideosUrls() {
        return videosUrls;
    }

    public void setVideosUrls(List<String> videosUrls) {
        this.videosUrls = videosUrls;
    }
}
