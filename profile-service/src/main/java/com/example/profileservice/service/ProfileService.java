package com.example.profileservice.service;

import com.example.profileservice.entity.Profile;
import com.example.profileservice.repository.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ProfileService {

    @Autowired
    private ProfileRepository profileRepository;

    public Profile createProfile(Long userId, String firstName, String lastName) {
        if (profileRepository.existsByUserId(userId)) {
            throw new RuntimeException("Profile already exists for user ID: " + userId);
        }

        Profile profile = new Profile(userId, firstName, lastName);
        return profileRepository.save(profile);
    }

    public Optional<Profile> getProfileByUserId(Long userId) {
        return profileRepository.findByUserId(userId);
    }

    public Profile updateProfile(Long userId, Profile updatedProfile) {
        Optional<Profile> existingProfile = profileRepository.findByUserId(userId);

        if (existingProfile.isPresent()) {
            Profile profile = existingProfile.get();

            if (updatedProfile.getFirstName() != null) {
                profile.setFirstName(updatedProfile.getFirstName());
            }
            if (updatedProfile.getLastName() != null) {
                profile.setLastName(updatedProfile.getLastName());
            }
            if (updatedProfile.getBio() != null) {
                profile.setBio(updatedProfile.getBio());
            }
            if (updatedProfile.getProfileImageUrl() != null) {
                profile.setProfileImageUrl(updatedProfile.getProfileImageUrl());
            }
            if (updatedProfile.getPhoneNumber() != null) {
                profile.setPhoneNumber(updatedProfile.getPhoneNumber());
            }
            if (updatedProfile.getLocation() != null) {
                profile.setLocation(updatedProfile.getLocation());
            }
            if (updatedProfile.getBirthDate() != null) {
                profile.setBirthDate(updatedProfile.getBirthDate());
            }

            profile.setUpdatedAt(LocalDateTime.now());
            return profileRepository.save(profile);
        } else {
            throw new RuntimeException("Profile not found for user ID: " + userId);
        }
    }

    public void deleteProfile(Long userId) {
        Optional<Profile> profile = profileRepository.findByUserId(userId);
        if (profile.isPresent()) {
            profileRepository.delete(profile.get());
        } else {
            throw new RuntimeException("Profile not found for user ID: " + userId);
        }
    }

  }