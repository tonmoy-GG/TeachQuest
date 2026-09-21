package com.teachquest.service;

import com.teachquest.model.ChatGroup;
import com.teachquest.model.ChatGroupMember;
import com.teachquest.model.User;
import com.teachquest.repository.ChatGroupMemberRepository;
import com.teachquest.repository.ChatGroupRepository;
import com.teachquest.repository.JobPostRepository;
import com.teachquest.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GroupChatService {
    private final ChatGroupRepository groupRepository;
    private final ChatGroupMemberRepository memberRepository;
    private final JobPostRepository jobPostRepository;
    private final UserRepository userRepository;

    public GroupChatService(ChatGroupRepository groupRepository,
                            ChatGroupMemberRepository memberRepository,
                            JobPostRepository jobPostRepository,
                            UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.jobPostRepository = jobPostRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public GroupView createGroup(Long ownerId, String name, List<Long> studentIds) {
        User owner = requireTeacher(ownerId);
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isBlank() || cleanName.length() > 150) {
            throw new IllegalArgumentException("A group name between 1 and 150 characters is required.");
        }

        ChatGroup group = new ChatGroup();
        group.setName(cleanName);
        group.setOwnerId(owner.getId());
        group = groupRepository.save(group);
        addMemberRecord(group.getId(), owner.getId(), "OWNER");

        if (studentIds != null) {
            for (Long studentId : studentIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList())) {
                addStudent(group, owner, studentId);
            }
        }
        return toView(group);
    }

    @Transactional(readOnly = true)
    public List<GroupView> getGroups(Long userId) {
        requireUser(userId);
        return memberRepository.findByUserId(userId).stream()
                .map(member -> groupRepository.findById(member.getGroupId()).orElse(null))
                .filter(Objects::nonNull)
                .filter(group -> "ACTIVE".equalsIgnoreCase(group.getStatus()))
                .sorted(Comparator.comparing(ChatGroup::getCreatedAt).reversed())
                .map(this::toView)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MemberView> getEligibleStudents(Long tutorId) {
        requireTeacher(tutorId);

        java.util.Set<Long> studentIds = new java.util.LinkedHashSet<>();
        studentIds.addAll(jobPostRepository.findStudentIdsByHiredTutorId(tutorId));
        studentIds.addAll(jobPostRepository.findStudentIdsByHiredTutorIdFromApplications(tutorId));

        return studentIds.stream()
                .map(userRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(user -> "student".equalsIgnoreCase(user.getUserType()))
                .map(this::toMemberView)
                .sorted(Comparator.comparing(MemberView::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    @Transactional
    public GroupView addStudent(Long groupId, Long requesterId, Long studentId) {
        ChatGroup group = requireActiveGroup(groupId);
        requireOwner(group, requesterId);
        User tutor = requireTeacher(requesterId);
        addStudent(group, tutor, studentId);
        return toView(group);
    }

    @Transactional
    public GroupView removeStudent(Long groupId, Long requesterId, Long studentId) {
        ChatGroup group = requireActiveGroup(groupId);
        requireOwner(group, requesterId);
        if (Objects.equals(group.getOwnerId(), studentId)) {
            throw new IllegalArgumentException("The group owner cannot be removed. Archive the group instead.");
        }
        if (memberRepository.findByGroupIdAndUserId(groupId, studentId).isEmpty()) {
            throw new IllegalArgumentException("That user is not a member of this group.");
        }
        memberRepository.deleteByGroupIdAndUserId(groupId, studentId);
        return toView(group);
    }

    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        ChatGroup group = requireActiveGroup(groupId);
        requireMember(groupId, userId);
        if (Objects.equals(group.getOwnerId(), userId)) {
            group.setStatus("ARCHIVED");
            groupRepository.save(group);
            return;
        }
        memberRepository.deleteByGroupIdAndUserId(groupId, userId);
    }

    public void requireMember(Long groupId, Long userId) {
        requireActiveGroup(groupId);
        if (memberRepository.findByGroupIdAndUserId(groupId, userId).isEmpty()) {
            throw new IllegalArgumentException("You are not a member of this group.");
        }
    }

    private void addStudent(ChatGroup group, User tutor, Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student account was not found."));
        if (!"student".equalsIgnoreCase(student.getUserType())) {
            throw new IllegalArgumentException("Only student accounts can join a group.");
        }
        boolean hired = jobPostRepository.existsByUserIdAndHiredTutorId(studentId, tutor.getId())
                || jobPostRepository.existsByUserIdAndHiredTutorIdViaApplication(studentId, tutor.getId());
        if (!hired) {
            throw new IllegalArgumentException("This student has not hired this tutor.");
        }
        if (memberRepository.findByGroupIdAndUserId(group.getId(), studentId).isEmpty()) {
            addMemberRecord(group.getId(), studentId, "MEMBER");
        }
    }

    private void addMemberRecord(Long groupId, Long userId, String role) {
        ChatGroupMember member = new ChatGroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(role);
        memberRepository.save(member);
    }

    private ChatGroup requireActiveGroup(Long groupId) {
        ChatGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group was not found."));
        if (!"ACTIVE".equalsIgnoreCase(group.getStatus())) {
            throw new IllegalArgumentException("This group is archived.");
        }
        return group;
    }

    public void requireOwner(ChatGroup group, Long userId) {
        if (!Objects.equals(group.getOwnerId(), userId)) {
            throw new IllegalArgumentException("Only the tutor who created this group can manage it.");
        }
    }

    private User requireTeacher(Long userId) {
        User user = requireUser(userId);
        if (!"teacher".equalsIgnoreCase(user.getUserType())) {
            throw new IllegalArgumentException("Only tutors can perform this action.");
        }
        return user;
    }

    private User requireUser(Long userId) {
        if (userId == null) throw new IllegalArgumentException("A valid user is required.");
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User account was not found."));
    }

    private GroupView toView(ChatGroup group) {
        List<MemberView> members = new ArrayList<>();
        for (ChatGroupMember member : memberRepository.findByGroupId(group.getId())) {
            userRepository.findById(member.getUserId()).ifPresent(user -> members.add(toMemberView(user, member.getRole())));
        }
        return new GroupView(group.getId(), group.getName(), group.getOwnerId(), group.getStatus(), group.getCreatedAt(), members);
    }

    private MemberView toMemberView(User user) { return toMemberView(user, "MEMBER"); }

    private MemberView toMemberView(User user, String role) {
        return new MemberView(user.getId(), user.getUsername(), user.getEmail(), user.getUserType(), role);
    }

    public static class GroupView {
        private final Long id;
        private final String name;
        private final Long ownerId;
        private final String status;
        private final java.time.LocalDateTime createdAt;
        private final List<MemberView> members;

        public GroupView(Long id, String name, Long ownerId, String status, java.time.LocalDateTime createdAt, List<MemberView> members) {
            this.id = id; this.name = name; this.ownerId = ownerId; this.status = status; this.createdAt = createdAt; this.members = members;
        }
        public Long getId() { return id; }
        public String getName() { return name; }
        public Long getOwnerId() { return ownerId; }
        public String getStatus() { return status; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public List<MemberView> getMembers() { return members; }
    }

    public static class MemberView {
        private final Long id;
        private final String name;
        private final String email;
        private final String userType;
        private final String role;

        public MemberView(Long id, String name, String email, String userType, String role) {
            this.id = id; this.name = name; this.email = email; this.userType = userType; this.role = role;
        }
        public Long getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getUserType() { return userType; }
        public String getRole() { return role; }
    }
}
