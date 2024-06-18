package com.simple.book.domain.project.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simple.book.domain.member.dto.request.MemberMappingToProjectRequestDto;
import com.simple.book.domain.member.entity.Member;
import com.simple.book.domain.member.repository.MemberRepository;
import com.simple.book.domain.member.repository.UserTaskRepository;
import com.simple.book.domain.member.service.MemberService;
import com.simple.book.domain.project.dto.request.CreateProjectRequestDto;
import com.simple.book.domain.project.dto.request.DeleteProjectRequestDto;
import com.simple.book.domain.project.dto.request.GetProjectsRequestDto;
import com.simple.book.domain.project.dto.response.GetProjectsResponseDto;
import com.simple.book.domain.project.entity.Project;
import com.simple.book.domain.project.repository.ProjectRepository;
import com.simple.book.domain.search.service.SearchService;
import com.simple.book.domain.task.entity.Task;
import com.simple.book.domain.task.repository.TaskRepository;
import com.simple.book.domain.user.entity.User;
import com.simple.book.domain.user.repository.UserRepository;
import com.simple.book.domain.user.service.UserService;
import com.simple.book.global.exception.EntityNotFoundException;
import com.simple.book.global.exception.InvalidValueException;
import com.simple.book.global.exception.UserNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectService {
	private final ProjectRepository projectRepository;
	private final UserService userService;
	private final UserRepository userRepository;
	private final MemberRepository memberRepository;
	private final TaskRepository taskRepository;
	private final UserTaskRepository userTaskRepository;
    private final MemberService memberService;
    private final SearchService searchService;
    private final InviteService inviteService;

	@Transactional(rollbackFor = { Exception.class })
	public Project createProject(CreateProjectRequestDto projectCreateRequestDto) {
		Project project = Project.builder()
                .description(projectCreateRequestDto.getDescription())
                .startDate(projectCreateRequestDto.getStartDate())
                .endDate(projectCreateRequestDto.getEndDate())
				.title(projectCreateRequestDto.getTitle()).build();
		projectRepository.save(project);
        MemberMappingToProjectRequestDto memberMappingToProjectRequestDto = MemberMappingToProjectRequestDto.builder()
                .projectId(project.getId())
                .userId(userService.getCurrentUserId())
                .isManager(true)
                .build();
        memberService.memberAddToProject(memberMappingToProjectRequestDto);
        
        // elasticsearch index 추가
        searchService.newInit(project);
        
        //초대 URL 생성
        inviteService.createLink(project);
        
		return project;
	}
    @Transactional(rollbackFor = { Exception.class })
    public List<GetProjectsResponseDto> getProjects(GetProjectsRequestDto getProjectsRequestDto) {
        try{
            User user = userRepository.findByAuthenticationUserId(getProjectsRequestDto.getUserId());
            List<Project> projects = memberRepository.findProjectsByUserId(user.getId());
            return projects.stream()
                    .map(project -> new GetProjectsResponseDto(
                            project.getId(),
                            project.getTitle(),
                            project.getDescription(),
                            project.getStartDate(),
                            project.getEndDate(),
                            project.getMembers().stream()
                                    .map(member -> member.getUser().getId())
                                    .collect(Collectors.toList())
                    ))
                    .collect(Collectors.toList());
        }catch (NullPointerException e){
            throw new UserNotFoundException(e.getMessage());
        }

    }
    @Transactional(rollbackFor = {Exception.class})
    public String deleteProject(DeleteProjectRequestDto projectDeleteRequestDto) {

        try{
            Optional<Project> opProject = projectRepository.findById(projectDeleteRequestDto.getProjectId());
            User user = userRepository.findByAuthenticationUserId(userService.getCurrentUserId());
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
        }catch (NullPointerException e) {
            throw new UserNotFoundException(e.getMessage());
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
                throw new InvalidValueException("해당 멤버는 해당 프로젝트의 관리자가 아닙니다. ProjectId : " + project.getId() + " UserId : " + user.getAuthentication().getUserId());
            }
        } else {
            throw new InvalidValueException("해당 멤버는 해당 프로젝트에 소속되어 있지 않습니다. ProjectId : " + project.getId() + " UserId : " + user.getAuthentication().getUserId());
        }
    }

}
