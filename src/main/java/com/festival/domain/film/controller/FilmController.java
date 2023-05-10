package com.festival.domain.film.controller;

import com.festival.domain.film.data.dto.FilmReq;
import com.festival.domain.film.data.dto.FilmRes;
import com.festival.domain.film.data.entity.Film;
import com.festival.domain.film.service.FilmService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FilmController {

    private final FilmService filmService;
    @PostMapping("/film")
    public Film createFilm(@RequestBody FilmReq filmReq){
        return filmService.create(filmReq, 1L);
    }
}
