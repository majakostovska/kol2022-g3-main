package mk.ukim.finki.wp.kol2022.g3.service.impl;

import mk.ukim.finki.wp.kol2022.g3.model.ForumUser;
import mk.ukim.finki.wp.kol2022.g3.model.ForumUserType;
import mk.ukim.finki.wp.kol2022.g3.model.Interest;
import mk.ukim.finki.wp.kol2022.g3.model.exceptions.InvalidForumUserIdException;
import mk.ukim.finki.wp.kol2022.g3.model.exceptions.InvalidInterestIdException;
import mk.ukim.finki.wp.kol2022.g3.repository.ForumUserRepository;
import mk.ukim.finki.wp.kol2022.g3.repository.InterestRepository;
import mk.ukim.finki.wp.kol2022.g3.service.ForumUserService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ForumUserServiceImpl implements ForumUserService, UserDetailsService {

    private final ForumUserRepository forumUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final InterestRepository interestRepository;

    public ForumUserServiceImpl(ForumUserRepository forumUserRepository, PasswordEncoder passwordEncoder, InterestRepository interestRepository) {
        this.forumUserRepository = forumUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.interestRepository = interestRepository;
    }

    @Override
    public List<ForumUser> listAll() {
        return this.forumUserRepository.findAll();
    }

    @Override
    public ForumUser findById(Long id) {
        return this.forumUserRepository.findById(id).orElseThrow(InvalidForumUserIdException::new);
    }

    @Override
    public ForumUser create(String name, String email, String password, ForumUserType type, List<Long> interestId, LocalDate birthday) {
        return this.forumUserRepository.save(new ForumUser(name,email,passwordEncoder.encode(password),type,this.interestRepository.findAllById(interestId),birthday));
    }

    @Override
    public ForumUser update(Long id, String name, String email, String password, ForumUserType type, List<Long> interestId, LocalDate birthday) {
        ForumUser forumUser = this.findById(id);
        forumUser.setName(name);
        forumUser.setEmail(email);
        forumUser.setPassword(passwordEncoder.encode(password));
        forumUser.setType(type);
        forumUser.setInterests(this.interestRepository.findAllById(interestId));
        forumUser.setBirthday(birthday);
        return this.forumUserRepository.save(forumUser);
    }

    @Override
    public ForumUser delete(Long id) {
        ForumUser forumUser = this.findById(id);
        this.forumUserRepository.delete(forumUser);
        return forumUser;
    }

    @Override
    public List<ForumUser> filter(Long interestId, Integer age) {
        if(interestId == null && age == null){
            return this.forumUserRepository.findAll();
        }
        else if (interestId == null)
        {
            return this.forumUserRepository.findAll().stream().filter(forumUser -> LocalDate.now().getYear() - forumUser.getBirthday().getYear() > age).collect(Collectors.toList());
        }
        else if(age == null){
            Interest interest = this.interestRepository.findById(interestId).orElseThrow(InvalidInterestIdException::new);
            return this.forumUserRepository.findByInterests(interest);
        }
        else {
            Interest interest = this.interestRepository.findById(interestId).orElseThrow(InvalidInterestIdException::new);
            return this.forumUserRepository.findByInterests(interest).stream().filter(forumUser -> LocalDate.now().getYear() - forumUser.getBirthday().getYear() > age).collect(Collectors.toList());
        }
    }

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        ForumUser user = this.forumUserRepository.findByEmail(s).orElseThrow(()-> new UsernameNotFoundException(s));
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Stream.of(new SimpleGrantedAuthority("ROLE_"+user.getType().toString())).collect(Collectors.toList()));

    }
}
