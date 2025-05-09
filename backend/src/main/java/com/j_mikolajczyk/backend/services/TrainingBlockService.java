package com.j_mikolajczyk.backend.services;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;

import com.j_mikolajczyk.backend.models.Day;
import com.j_mikolajczyk.backend.models.TrainingBlock;
import com.j_mikolajczyk.backend.models.User;
import com.j_mikolajczyk.backend.models.Week;
import com.j_mikolajczyk.backend.repositories.TrainingBlockRepository;
import com.j_mikolajczyk.backend.requests.UpdateBlockRequest;
import com.j_mikolajczyk.backend.requests.AddWeekRequest;
import com.j_mikolajczyk.backend.requests.CreateTrainingBlockRequest;

@Service
public class TrainingBlockService {

    private final TrainingBlockRepository blockRepository;
    private final UserService userService;

    @Autowired
    public TrainingBlockService(TrainingBlockRepository blockRepository, UserService userService) {
        this.blockRepository = blockRepository;
        this.userService = userService;
    }

    public TrainingBlock get(String blockName, ObjectId id) throws Exception{
        if(blockName == null || id == null) {
            throw new RuntimeException("Email and Block Name are required.");
        }

        ObjectId userId = null;

        try {
            User user = userService.getById(id);
            userId = user.getId();
        } catch(Exception e) {
            throw new NotFoundException();
        }

        Optional<TrainingBlock> fetchedBlock = blockRepository.findByNameAndCreatedByUserID(blockName, userId);

        if (fetchedBlock.isPresent()) {
            return fetchedBlock.get();
        } else {
            throw new NotFoundException();
        }
    }


    public TrainingBlock get(ObjectId id) throws Exception{
        if(id == null) {
            throw new RuntimeException("Email and Block Name are required.");
        }

        Optional<TrainingBlock> fetchedBlock = blockRepository.findById(id);

        if (fetchedBlock.isPresent()) {
            return fetchedBlock.get();
        } else {
            throw new NotFoundException();
        }
    }

    public void create(CreateTrainingBlockRequest createBlockRequest) throws Exception{

        if(createBlockRequest.getUserId() == null) {
            throw new RuntimeException("UserID is required.");
        }

        TrainingBlock block = new TrainingBlock(createBlockRequest.getBlockName(), createBlockRequest.getUserId(), createBlockRequest.getSortedDays());

        try {
            blockRepository.save(block);
            userService.addBlock(block);
        } catch (Exception e) {
            blockRepository.delete(block);
            throw e;
        }

    }

    public void update(UpdateBlockRequest updateBlockRequest) throws Exception{
        ObjectId id = updateBlockRequest.getId();

        if(id == null) {
            throw new RuntimeException("Block ID and Block Name is required.");
        }

        try {
            TrainingBlock block = this.get(id);
            block.setName(updateBlockRequest.getName());
            block.setWeeks(updateBlockRequest.getWeeks());
            blockRepository.save(block);
        } catch (Exception e) {
            throw e;
        }

    }

    public Day getDay(String stringId, int weekIndex, int dayIndex) throws Exception{
        if(stringId == null) {
            throw new RuntimeException("Block ID and Block Name is required.");
        }
        
        ObjectId blockId = new ObjectId(stringId);

        try {
            TrainingBlock block = this.get(blockId);
            List<Week> weeks = block.getWeeks();
            Week week = weeks.get(weekIndex);

            block.setMostRecentDayOpen(dayIndex);
            block.setMostRecentWeekOpen(weekIndex);
            blockRepository.save(block);
            System.out.println("Set most recent day/week for block " + block.getName() + ", returning day");
            return week.getDays().get(dayIndex);
        } catch (Exception e) {
            throw e;
        }

    }
}
