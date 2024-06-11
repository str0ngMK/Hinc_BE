package com.simple.book.domain.project.service;

import com.simple.book.domain.alarm.dto.req.ReqTopicDto;
import com.simple.book.domain.alarm.service.AlarmService;
import com.simple.book.domain.member.entity.Member;
import com.simple.book.domain.member.repository.MemberRepository;
import com.simple.book.domain.member.repository.UserTaskRepository;
import com.simple.book.domain.project.dto.request.ProjectCreateRequestDto;
import com.simple.book.domain.project.dto.request.ProjectDeleteRequestDto;
import com.simple.book.domain.project.entity.Project;
import com.simple.book.domain.project.repository.ProjectRepository;
import com.simple.book.domain.task.entity.Task;
import com.simple.book.domain.task.repository.TaskRepository;
import com.simple.book.domain.user.entity.User;
import com.simple.book.domain.user.repository.UserRepository;
import com.simple.book.domain.user.service.UserService;
import com.simple.book.global.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectService {
	@Autowired
	private AlarmService alarmService;
	
	private final String TYPE = "P"; // project = P, task = T
	
    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final TaskRepository taskRepository;
    private final UserTaskRepository userTaskRepository;
	
    @Transactional(rollbackFor = {Exception.class})
    public String createProject(ProjectCreateRequestDto projectCreateRequestDto){
        Project project = Project.builder()
                .description(projectCreateRequestDto.getDescription())
                .title(projectCreateRequestDto.getTitle())
                .build();
        long id = projectRepository.save(project).getId();

        alarmService.createTopic(setDto(id));
        return "OK";
    }
    
    private ReqTopicDto setDto(long id) {
    	ReqTopicDto dto = new ReqTopicDto();
    	dto.setName(TYPE + id);
    	dto.setType(TYPE);
    	dto.setId(id);
    	return dto;
    }
    @Transactional(rollbackFor = {Exception.class})
    public String deleteProject(ProjectDeleteRequestDto projectDeleteRequestDto) {
        Optional<Project> opProject = projectRepository.findById(projectDeleteRequestDto.getProjectId());

        User user = userRepository.findByAuthenticationUserId(userService.getCurrentUserId());


        try{
            if (opProject.isPresent()){
                Project project = opProject.get();
                project.getTasks().size();
                isProjectManager(user, project);
                // 연관된 task 삭제
                List<Task> tasks = project.getTasks();
                for (Task task : tasks) {
                    task.getTaskMembers().clear();
                    userTaskRepository.deleteByTaskId(task.getId());


                    taskRepository.delete(task);
                }

                // 연관된 member 삭제
                List<Member> members = project.getMembers();
                for (Member member : members) {
                    member.getTaskMembers().clear();
                    memberRepository.delete(member);
                }


                projectRepository.delete(project);
            }else {
                throw new EntityNotFoundException("해당 프로젝트는 존재하지 않습니다. ProjectId : " + projectDeleteRequestDto.getProjectId());
            }
        }catch (Exception e) {
            throw new RuntimeException(e);
        }


        return "OK";
    }
    //프로젝트의 관리자인지 확인합니다.
    public void isProjectManager(User user, Project project) {
        Long userId = user.getId();

        Optional<Member> member = memberRepository.findByUserIdAndProjectId(userId, project.getId());

        if (member.isPresent()) {
            if (member.get().isManager()) {
                // 유효한 관리자
            } else {
                throw new EntityNotFoundException("해당 멤버는 해당 프로젝트의 관리자가 아닙니다. ProjectId : " + project.getId() + " UserId : " + user.getAuthentication().getUserId());
            }
        } else {
            throw new EntityNotFoundException("해당 멤버는 해당 프로젝트에 소속되어 있지 않습니다. ProjectId : " + project.getId() + " UserId : " + user.getAuthentication().getUserId());
        }
    }

}
