package com.weekahead.lifearea.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.weekahead.auth.entity.User;
import com.weekahead.auth.service.CurrentUserService;
import com.weekahead.lifearea.dto.LifeAreaRequest;
import com.weekahead.lifearea.dto.LifeAreaResponse;
import com.weekahead.lifearea.entity.LifeArea;
import com.weekahead.lifearea.repository.LifeAreaRepository;

@Service
public class LifeAreaService {

    private final LifeAreaRepository lifeAreaRepository;
    private final CurrentUserService currentUserService;

    public LifeAreaService(
            LifeAreaRepository lifeAreaRepository,
            CurrentUserService currentUserService
    ) {
        this.lifeAreaRepository = lifeAreaRepository;
        this.currentUserService = currentUserService;
    }

    public LifeAreaResponse create(LifeAreaRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        if (request.maxMinutes() < request.minMinutes()) {
            throw new IllegalArgumentException(
                    "Maximum minutes must be greater than or equal to minimum minutes"
            );
        }

        LifeArea lifeArea = new LifeArea(
                currentUser,
                request.name().trim(),
                request.description(),
                request.weight(),
                request.minMinutes(),
                request.maxMinutes()
        );

        LifeArea savedLifeArea = lifeAreaRepository.save(lifeArea);

        return toResponse(savedLifeArea);
    }

    public List<LifeAreaResponse> getAll() {
        User currentUser = currentUserService.getCurrentUser();

        return lifeAreaRepository.findAllByUserId(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public LifeAreaResponse update(Long id, LifeAreaRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        if (request.maxMinutes() < request.minMinutes()) {
            throw new IllegalArgumentException(
                    "Maximum minutes must be greater than or equal to minimum minutes"
            );
        }

        LifeArea lifeArea = lifeAreaRepository
                .findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Life area not found"));

        lifeArea.setName(request.name().trim());
        lifeArea.setDescription(request.description());
        lifeArea.setWeight(request.weight());
        lifeArea.setMinMinutes(request.minMinutes());
        lifeArea.setMaxMinutes(request.maxMinutes());

        LifeArea updatedLifeArea = lifeAreaRepository.save(lifeArea);

        return toResponse(updatedLifeArea);
    }

    public void delete(Long id) {
        User currentUser = currentUserService.getCurrentUser();

        LifeArea lifeArea = lifeAreaRepository
                .findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Life area not found"));

        lifeArea.setIsActive(false);
        lifeAreaRepository.save(lifeArea);
    }

    private LifeAreaResponse toResponse(LifeArea lifeArea) {
        return new LifeAreaResponse(
                lifeArea.getId(),
                lifeArea.getName(),
                lifeArea.getDescription(),
                lifeArea.getWeight(),
                lifeArea.getMinMinutes(),
                lifeArea.getMaxMinutes(),
                lifeArea.getIsActive(),
                lifeArea.getCreatedAt(),
                lifeArea.getUpdatedAt()
        );
    }
}
