package controle.finaceiro.api.domain.transacao;

import controle.finaceiro.api.domain.categoria.Categoria;
import controle.finaceiro.api.domain.categoria.CategoriaRepository;
import controle.finaceiro.api.service.UnauthorizedException;
import controle.finaceiro.api.user.User;
import controle.finaceiro.api.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TransacaoService {
    @Autowired
    private TransacaoRepository transacaoRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;

    public Transacao createTransacao(TransacaoDTO transacaoDto) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));
        Categoria categoria = categoriaRepository.findById(transacaoDto.getCategoriaId())
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada: " + transacaoDto.getCategoriaId()));

        Transacao transacao = new Transacao();
        transacao.setUser(user);
        transacao.setCategoria(categoria);

        BeanUtils.copyProperties(transacaoDto, transacao, "categoriaId");

        return transacaoRepository.save(transacao);
    }

    public Transacao updateTransacao(Long id, TransacaoDTO transacaoDetails) {
        Transacao transacao = getTransacaoById(id);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        if (!transacao.getUser().equals(user)) {
            throw new UnauthorizedException("O usuário não tem permissão para atualizar esta transação");
        }

        Categoria categoria = categoriaRepository.findById(transacaoDetails.getCategoriaId())
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada: " + transacaoDetails.getCategoriaId()));

        transacao.setCategoria(categoria);

        return transacaoRepository.save(transacao);
    }

    public Page<Transacao> getAllTransacoes(Pageable pageable) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));
        Page<Transacao> transacoes = transacaoRepository.findByUser(user, pageable);
        transacoes.getContent().forEach(t -> t.getCategoria().getNome()); // Força a inicialização de Categoria
        return transacoes;
    }

    public Page<Transacao> getAllTransacoesAdmin(Pageable pageable) {
        return transacaoRepository.findAll(pageable);
    }

    public Transacao getTransacaoById(Long id) {
        Optional<Transacao> transacaoOpt = transacaoRepository.findById(id);
        if (transacaoOpt.isPresent()) {
            Transacao transacao = transacaoOpt.get();
            transacao.getCategoria().getNome(); // Força a inicialização de Categoria
            return transacao;
        } else {
            throw new EntityNotFoundException("Transação não encontrada com o id " + id);
        }
    }

    public void deleteTransacao(Long id) {
        transacaoRepository.delete(getTransacaoById(id));
    }

    public List<Transacao> getTransacoesByCategoria(Long categoriaId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new EntityNotFoundException("Categoria não encontrada: " + categoriaId));
        return transacaoRepository.findByUserAndCategoria(user, categoria);
    }
}
