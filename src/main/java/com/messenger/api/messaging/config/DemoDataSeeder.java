package com.messenger.api.messaging.config;

import com.messenger.api.messaging.domain.AppUser;
import com.messenger.api.messaging.domain.Conversation;
import com.messenger.api.messaging.domain.ConversationMember;
import com.messenger.api.messaging.domain.ConversationType;
import com.messenger.api.messaging.domain.MemberRole;
import com.messenger.api.messaging.domain.Message;
import com.messenger.api.messaging.domain.MessageReceipt;
import com.messenger.api.messaging.domain.MessageStatus;
import com.messenger.api.messaging.repository.AppUserRepository;
import com.messenger.api.messaging.repository.ConversationMemberRepository;
import com.messenger.api.messaging.repository.ConversationRepository;
import com.messenger.api.messaging.repository.MessageReceiptRepository;
import com.messenger.api.messaging.repository.MessageRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataSeeder implements CommandLineRunner {
    private final AppUserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final MessageReceiptRepository receiptRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(
        AppUserRepository userRepository,
        ConversationRepository conversationRepository,
        ConversationMemberRepository memberRepository,
        MessageRepository messageRepository,
        MessageReceiptRepository receiptRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
        this.receiptRepository = receiptRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        AppUser current = saveUser(
            "user-current",
            "You",
            "sishirshrestha0",
            "sishir@example.com",
            "password123",
            null,
            true,
            null
        );
        AppUser aanya = saveUser(
            "user-aanya",
            "Aanya Sharma",
            "aanya_sharma",
            "aanya@example.com",
            "password123",
            null,
            true,
            null
        );
        AppUser bibek = saveUser(
            "user-bibek",
            "Bibek K.C.",
            "bibek_kc",
            "bibek@example.com",
            "password123",
            null,
            false,
            minutesAgo(22)
        );
        AppUser maya = saveUser(
            "user-maya",
            "Maya Gurung",
            "maya_gurung",
            "maya@example.com",
            "password123",
            null,
            true,
            null
        );
        AppUser product = saveUser(
            "user-product",
            "Product Team",
            "product_team",
            "product@example.com",
            "password123",
            null,
            false,
            minutesAgo(90)
        );

        if (conversationRepository.count() == 0) {
            Conversation aanyaConversation = createConversation(
                ConversationType.DIRECT,
                null,
                current,
                List.of(current, aanya)
            );
            saveMessage(aanyaConversation, aanya, "Hey, did you get a chance to review the chat layout?");
            saveMessage(
                aanyaConversation,
                current,
                "Yes. I am simplifying the desktop view and keeping mobile focused on one panel at a time."
            );
            saveMessage(aanyaConversation, aanya, "Nice. Can you send the latest mockup?");

            Conversation bibekConversation = createConversation(
                ConversationType.DIRECT,
                null,
                current,
                List.of(current, bibek)
            );
            saveMessage(bibekConversation, bibek, "I pushed the API contract changes.");
            saveMessage(
                bibekConversation,
                current,
                "Good. I will keep the frontend service layer clean so the real endpoints can drop in later."
            );

            Conversation mayaConversation = createConversation(
                ConversationType.DIRECT,
                null,
                current,
                List.of(current, maya)
            );
            saveMessage(mayaConversation, maya, "Let me know once the frontend route is ready.");

            Conversation teamConversation = createConversation(
                ConversationType.GROUP,
                "Product Team",
                current,
                List.of(current, aanya, bibek, maya, product)
            );
            saveMessage(teamConversation, product, "Sprint review is moved to 4:00 PM.");
            saveMessage(teamConversation, current, "Thanks, I will update the calendar invite.");
        }

        ensureDirectConversation(
            aanya,
            bibek,
            List.of(
                new DemoMessage(aanya, "Can you check the unread count edge case?"),
                new DemoMessage(bibek, "Yes, I found the nullable timestamp problem in Postgres."),
                new DemoMessage(aanya, "Perfect. I will test the new message popup after restart.")
            )
        );
        ensureDirectConversation(
            maya,
            aanya,
            List.of(
                new DemoMessage(maya, "The login page needs email and password now."),
                new DemoMessage(aanya, "I added the seeded account list to make testing faster.")
            )
        );
        ensureDirectConversation(
            bibek,
            maya,
            List.of(
                new DemoMessage(bibek, "Do you have the PostgreSQL schema ready?"),
                new DemoMessage(maya, "Yes, the groupapi schema has the messaging tables.")
            )
        );
    }

    private AppUser saveUser(
        String id,
        String name,
        String username,
        String email,
        String password,
        String avatarUrl,
        boolean online,
        Instant lastActiveAt
    ) {
        AppUser user = userRepository.findById(id)
            .orElseGet(() -> new AppUser(id, name, username, email, passwordEncoder.encode(password), avatarUrl));
        user.setOnline(online);
        user.setName(name);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setAvatarUrl(avatarUrl);
        user.setLastActiveAt(lastActiveAt);
        return userRepository.save(user);
    }

    private Conversation createConversation(
        ConversationType type,
        String name,
        AppUser createdBy,
        List<AppUser> members
    ) {
        Conversation conversation = conversationRepository.save(new Conversation(type, name, null, createdBy));
        members.forEach(user -> memberRepository.save(new ConversationMember(conversation, user, MemberRole.MEMBER)));
        return conversation;
    }

    private void saveMessage(Conversation conversation, AppUser sender, String body) {
        Message message = messageRepository.save(new Message(conversation, sender, body));
        conversation.setLastMessage(message);
        conversation.setLastMessageAt(message.getCreatedAt());
        conversationRepository.save(conversation);

        memberRepository.findByConversationIdAndDeletedAtIsNullOrderByJoinedAtAsc(conversation.getId()).stream()
            .map(ConversationMember::getUser)
            .filter(user -> !user.getId().equals(sender.getId()))
            .forEach(user -> receiptRepository.save(new MessageReceipt(message, user, MessageStatus.DELIVERED)));
    }

    private Instant minutesAgo(long minutes) {
        return Instant.now().minus(minutes, ChronoUnit.MINUTES);
    }

    private void ensureDirectConversation(AppUser firstUser, AppUser secondUser, List<DemoMessage> messages) {
        if (!conversationRepository
            .findDirectBetween(ConversationType.DIRECT, firstUser.getId(), secondUser.getId())
            .isEmpty()) {
            return;
        }

        Conversation conversation = createConversation(
            ConversationType.DIRECT,
            null,
            firstUser,
            List.of(firstUser, secondUser)
        );
        messages.forEach(message -> saveMessage(conversation, message.sender(), message.body()));
    }

    private record DemoMessage(AppUser sender, String body) {
    }
}
